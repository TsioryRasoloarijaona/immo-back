package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.FiltresRendezVous;
import com.tsix.immocrm.dto.administration.RendezVousDto;
import com.tsix.immocrm.dto.administration.RendezVousEcritureDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.entities.adm.AdmTask;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmTaskRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.str.StrEntityRepository;
import com.tsix.immocrm.services.patrimoine.StkItemSpecifications;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.web.export.LignesAplaties;
import com.tsix.immocrm.web.export.LignesExport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Rendez-vous d'OMRAN : lignes {@code AdmTask} de l'entité {@code AdmAppointment}. FEATURE-admin-rendez-vous.md. */
@Service
public class RendezVousService {

    private static final String SEQUENCE = "AdmTask";
    private static final String DATE_PREVUE = "startDatePrev";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final Sort TRI = Sort.by(DATE_PREVUE, CODE);
    private static final int PLAGE_MAX_JOURS = 62;
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AdmTaskRepository rendezVous;
    private final CfgTierRepository tiers;
    private final StrEntityRepository entites;
    private final EcritureRendezVous ecriture;
    private final GenerateurCodeSequence sequences;
    private final EntityManager em;

    public RendezVousService(AdmTaskRepository rendezVous, CfgTierRepository tiers, StrEntityRepository entites,
                             EcritureRendezVous ecriture, GenerateurCodeSequence sequences, EntityManager em) {
        this.rendezVous = rendezVous;
        this.tiers = tiers;
        this.entites = entites;
        this.ecriture = ecriture;
        this.sequences = sequences;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public PageDto<RendezVousDto> lister(FiltresRendezVous filtres, Pageable pageable) {
        Page<AdmTask> page = lire(filtres, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), TRI));
        Liens liens = liens(page.getContent());
        return PageDto.of(page.map(liens::dto));
    }

    @Transactional(readOnly = true)
    public List<RendezVousDto> calendrier(LocalDate debut, LocalDate fin, UUID gestionnaireId) {
        if (fin.isBefore(debut) || ChronoUnit.DAYS.between(debut, fin) > PLAGE_MAX_JOURS) {
            throw new InvalidRequestException("La plage va de debut à fin, %d jours au plus.".formatted(PLAGE_MAX_JOURS));
        }
        return dtos(rendezVous.findAll(specification(new FiltresRendezVous(null, gestionnaireId, debut, fin)), TRI));
    }

    /** Dates en texte : le style de date du classeur masquerait l'heure du rendez-vous. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> lignesExport(FiltresRendezVous filtres) {
        return dtos(lire(filtres, PageRequest.of(0, LignesExport.LIGNES_EXPORT_MAX, TRI)).getContent()).stream()
                .map(RendezVousService::ligneExport)
                .toList();
    }

    @Transactional(readOnly = true)
    public RendezVousDto detail(UUID id) {
        return dto(rendezVous(id));
    }

    @Transactional
    public RendezVousDto creer(RendezVousEcritureDto corps) {
        AdmTask r = new AdmTask();
        r.setId(UUID.randomUUID());
        r.setStrEntityId(AdmTask.ENTITE_RENDEZ_VOUS);
        ecriture.creer(r, corps);
        r.setCode(sequences.suivantSelonLigne(SEQUENCE));
        em.persist(r);
        return dto(r);
    }

    @Transactional
    public RendezVousDto modifier(UUID id, RendezVousEcritureDto corps) {
        AdmTask r = rendezVous(id);
        ecriture.modifier(r, corps);
        return dto(r);
    }

    @Transactional
    public void supprimer(UUID id) {
        rendezVous.delete(rendezVous(id));
    }

    /** Une tâche partage la table : même 404 qu'un rendez-vous inconnu. */
    private AdmTask rendezVous(UUID id) {
        return rendezVous.findByIdAndStrEntityId(id, AdmTask.ENTITE_RENDEZ_VOUS)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable: " + id));
    }

    private Page<AdmTask> lire(FiltresRendezVous filtres, Pageable pageable) {
        return rendezVous.findAll(specification(filtres), pageable);
    }

    private static Specification<AdmTask> specification(FiltresRendezVous f) {
        LocalDate debut = f.dateDebut();
        LocalDate fin = f.dateFin();
        if (debut != null && fin != null && fin.isBefore(debut)) {
            throw new InvalidRequestException("dateDebut doit précéder ou égaler dateFin.");
        }
        return (root, query, cb) -> {
            List<Predicate> criteres = new ArrayList<>();
            criteres.add(cb.equal(root.get("strEntityId"), AdmTask.ENTITE_RENDEZ_VOUS));
            if (f.gestionnaireId() != null) {
                criteres.add(cb.equal(root.get("gestionnaire").get("id"), f.gestionnaireId()));
            }
            if (debut != null) {
                criteres.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get(DATE_PREVUE), debut.atStartOfDay()));
            }
            if (fin != null) {
                criteres.add(cb.lessThan(root.<LocalDateTime>get(DATE_PREVUE), fin.plusDays(1).atStartOfDay()));
            }
            String saisie = f.texte() != null ? f.texte().replace(" ", "") : "";
            if (!saisie.isEmpty()) {
                criteres.add(texte(root, cb, HistoriqueService.motif(saisie).toLowerCase(Locale.ROOT)));
            }
            return cb.and(criteres.toArray(Predicate[]::new));
        };
    }

    private static Predicate texte(Root<AdmTask> root, CriteriaBuilder cb, String motif) {
        Join<AdmTask, CfgTier> declencheur = root.join("declencheur", JoinType.LEFT);
        return cb.or(Stream.of(root.<String>get(CODE), root.<String>get(DESCRIPTION), root.<String>get("objectLabel"),
                        declencheur.<String>get(CODE), declencheur.<String>get(DESCRIPTION))
                .map(colonne -> cb.like(StkItemSpecifications.sansEspaces(cb, colonne), motif))
                .toArray(Predicate[]::new));
    }

    private RendezVousDto dto(AdmTask r) {
        return liens(List.of(r)).dto(r);
    }

    private List<RendezVousDto> dtos(List<AdmTask> lignes) {
        Liens liens = liens(lignes);
        return lignes.stream().map(liens::dto).toList();
    }

    /** Une lecture par lot de tiers et d'entités : ni l'un ni l'autre n'est une relation JPA d'{@code AdmTask}. */
    private Liens liens(List<AdmTask> lignes) {
        return new Liens(
                parId(LotsTranches.lire(distincts(lignes, AdmTask::getCfgProspectId), tiers::findAllById), CfgTier::getId),
                parId(LotsTranches.lire(distincts(lignes, AdmTask::getObjectStrEntityId), entites::findByIdIn), StrEntity::getId));
    }

    private static List<UUID> distincts(List<AdmTask> lignes, Function<AdmTask, UUID> colonne) {
        return lignes.stream().map(colonne).filter(Objects::nonNull).distinct().toList();
    }

    private static <E> Map<UUID, E> parId(List<E> lignes, Function<E, UUID> id) {
        return lignes.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private static Map<String, Object> ligneExport(RendezVousDto ligne) {
        Map<String, Object> valeurs = LignesAplaties.de(ligne);
        valeurs.put("datePrevue", Champs.de(ligne.datePrevue(), DATE_HEURE::format));
        valeurs.put("dateFinPrevue", Champs.de(ligne.dateFinPrevue(), DATE_HEURE::format));
        return valeurs;
    }

    private record Liens(Map<UUID, CfgTier> tiers, Map<UUID, StrEntity> entites) {

        RendezVousDto dto(AdmTask r) {
            CfgTier declencheur = r.getDeclencheur();
            CfgTier gestionnaire = r.getGestionnaire();
            CfgTier tier = tiers.get(r.getCfgProspectId());
            return new RendezVousDto(r.getId(), r.getCode(), r.getDescription(),
                    Champs.de(declencheur, CfgTier::getId), Champs.de(declencheur, CfgTier::getCode),
                    Champs.de(declencheur, CfgTier::getDescription),
                    r.getStartDatePrev(), r.getDelayPrev(), r.getDueDatePrev(),
                    Champs.de(gestionnaire, CfgTier::getId), Champs.de(gestionnaire, CfgTier::getCode),
                    Champs.de(gestionnaire, CfgTier::getDescription),
                    r.getCfgProspectId(), Champs.de(tier, CfgTier::getCode), Champs.de(tier, CfgTier::getDescription),
                    r.getObjectId(), r.getObjectLabel(),
                    Champs.de(entites.get(r.getObjectStrEntityId()), StrEntity::getDescription), r.getNotes());
        }
    }
}
