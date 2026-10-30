package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.ChangerStatutTacheRequest;
import com.tsix.immocrm.dto.administration.FiltresTaches;
import com.tsix.immocrm.dto.administration.StatutTacheDto;
import com.tsix.immocrm.dto.administration.TacheDto;
import com.tsix.immocrm.dto.administration.TacheEcritureDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.adm.AdmModule;
import com.tsix.immocrm.entities.adm.AdmTask;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmModuleRepository;
import com.tsix.immocrm.repositories.adm.AdmTaskRepository;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TachesService {

    private static final String SEQUENCE = "AdmTask";
    private static final Sort TRI = Sort.by("startDatePrev", "code");
    private static final String STATUT = "statut";
    private static final String ID = "id";

    private final AdmTaskRepository taches;
    private final AdmModuleRepository modules;
    private final TransitionsTache transitions;
    private final EcritureTache ecriture;
    private final GenerateurCodeSequence sequences;
    private final EntityManager em;

    public TachesService(AdmTaskRepository taches, AdmModuleRepository modules, TransitionsTache transitions,
                         EcritureTache ecriture, GenerateurCodeSequence sequences, EntityManager em) {
        this.taches = taches;
        this.modules = modules;
        this.transitions = transitions;
        this.ecriture = ecriture;
        this.sequences = sequences;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public PageDto<TacheDto> lister(FiltresTaches filtres, Pageable pageable) {
        UUID entite = transitions.entite();
        StrStatus initial = transitions.statutInitial(entite);
        Pageable trie = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), TRI);
        return PageDto.of(taches.findAll(specification(entite, initial, filtres), trie).map(t -> dto(t, initial)));
    }

    @Transactional(readOnly = true)
    public List<StatutTacheDto> statuts() {
        return transitions.catalogue();
    }

    @Transactional(readOnly = true)
    public List<ReferentielItemDto> modules() {
        return modules.findAllByOrderByCodeAsc().stream()
                .map(m -> new ReferentielItemDto(m.getId(), m.getCode(), m.getDescription(), null)).toList();
    }

    @Transactional(readOnly = true)
    public TacheDto detail(UUID id) {
        return dto(tache(id));
    }

    @Transactional
    public TacheDto creer(TacheEcritureDto corps) {
        UUID entite = transitions.entite();
        AdmTask t = new AdmTask();
        t.setId(UUID.randomUUID());
        t.setStrEntityId(entite);
        ecriture.appliquer(t, corps);
        t.setStatut(transitions.statutInitial(entite));
        t.setCode(sequences.suivantSelonLigne(SEQUENCE));
        ecriture.tracerCreation(t);
        em.persist(t);
        return dto(t);
    }

    @Transactional
    public TacheDto modifier(UUID id, TacheEcritureDto corps) {
        AdmTask t = tache(id);
        ecriture.appliquer(t, corps);
        ecriture.tracerModification(t);
        return dto(t);
    }

    @Transactional
    public void supprimer(UUID id) {
        taches.delete(tache(id));
    }

    @Transactional
    public TacheDto changerStatut(UUID id, ChangerStatutTacheRequest corps) {
        AdmTask t = tache(id);
        if (corps.statutId() == null) {
            throw new InvalidRequestException("statutId : obligatoire.");
        }
        transitions.executer(t, corps.statutId(), corps.commentaire());
        ecriture.tracerModification(t);
        return dto(t);
    }

    /** Un rendez-vous partage la table : même 404 qu'une tâche inconnue. */
    private AdmTask tache(UUID id) {
        return taches.findByIdAndStrEntityId(id, transitions.entite())
                .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable: " + id));
    }

    private static Specification<AdmTask> specification(UUID entite, StrStatus initial, FiltresTaches f) {
        return (root, query, cb) -> {
            List<Predicate> criteres = new ArrayList<>();
            criteres.add(cb.equal(root.get("strEntityId"), entite));
            if (f.statutId() != null) {
                criteres.add(statut(root, cb, f.statutId(), initial));
            }
            ajouter(criteres, root, cb, "type", f.typeId());
            ajouter(criteres, root, cb, "priorite", f.prioriteId());
            ajouter(criteres, root, cb, "gestionnaire", f.gestionnaireId());
            if (f.texte() != null && !f.texte().isBlank()) {
                String motif = "%" + f.texte().trim().toLowerCase(Locale.ROOT) + "%";
                criteres.add(cb.or(cb.like(cb.lower(root.get("code")), motif),
                        cb.like(cb.lower(root.get("description")), motif)));
            }
            return cb.and(criteres.toArray(Predicate[]::new));
        };
    }

    private static Predicate statut(Root<AdmTask> root, CriteriaBuilder cb, UUID statutId, StrStatus initial) {
        Predicate egal = cb.equal(root.get(STATUT).get(ID), statutId);
        return statutId.equals(initial.getId()) ? cb.or(egal, cb.isNull(root.get(STATUT))) : egal;
    }

    private static void ajouter(List<Predicate> criteres, Root<AdmTask> root, CriteriaBuilder cb, String relation, UUID id) {
        if (id != null) {
            criteres.add(cb.equal(root.get(relation).get(ID), id));
        }
    }

    private TacheDto dto(AdmTask t) {
        return dto(t, t.getStatut() != null ? t.getStatut() : transitions.statutInitial(t.getStrEntityId()));
    }

    /** {@code initial} : statut affiché d'une tâche sans statut. */
    private static TacheDto dto(AdmTask t, StrStatus initial) {
        StrStatus statut = t.getStatut() != null ? t.getStatut() : initial;
        return new TacheDto(t.getId(), t.getCode(), t.getDescription(),
                Champs.de(t.getType(), CodeLibelleOmran::getId), Champs.de(t.getType(), CodeLibelleOmran::getDescription),
                Champs.de(t.getDeclencheur(), CfgTier::getId), Champs.de(t.getDeclencheur(), CfgTier::getDescription),
                t.getStartDatePrev(), t.getDelayPrev(), t.getDueDatePrev(), t.getStartDate(), t.getDelay(), t.getEndDate(),
                Champs.de(t.getModule(), AdmModule::getId),
                Champs.de(t.getPriorite(), CodeLibelleOmran::getId), Champs.de(t.getPriorite(), CodeLibelleOmran::getDescription),
                Champs.de(t.getGestionnaire(), CfgTier::getId), Champs.de(t.getGestionnaire(), CfgTier::getDescription),
                Champs.de(t.getAssigne(), CfgTier::getId), Champs.de(t.getAssigne(), CfgTier::getDescription),
                Champs.de(t.getCfgCompany(), CfgCompany::getId), Champs.de(t.getPrjProject(), PrjProject::getId),
                t.getNotes(), statut.getId(), statut.getDescription());
    }
}
