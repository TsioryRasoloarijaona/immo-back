package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.dto.projet.PrevisionsChantierDto;
import com.tsix.immocrm.dto.projet.PrevisionsChantierDto.LignePrevisionChantierDto;
import com.tsix.immocrm.dto.projet.PrevisionsChantierDto.TypeTravauxDto;
import com.tsix.immocrm.dto.projet.PrevisionsChantierRequest;
import com.tsix.immocrm.dto.projet.PrevisionsChantierRequest.LignePrevisionChantierSaisie;
import com.tsix.immocrm.entities.grh.GrhWorkedDayType;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.prj.PrjProjectBuildingPrevision;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.prj.PrjProjectRepository;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;

/** Prévisions chantier de la fiche projet de configuration (FEATURE-n4-previsions-chantier.md N4b). */
@Service
@Transactional(readOnly = true)
public class PrevisionsChantierService {

    static final String DROIT = "Yes.Prj.Front.Definition.Project.ProjectList.BuildingPrevision";
    /** {@code decimal(26,6)} : 20 chiffres avant la virgule au plus. */
    private static final int CHIFFRES_ENTIERS = 20;
    private static final int NOTES_MAX = 1000;

    private final PrjProjectRepository projets;
    private final DroitsOmran droits;
    private final TracePatrimoine trace;
    private final EntityManager em;

    public PrevisionsChantierService(PrjProjectRepository projets, DroitsOmran droits, TracePatrimoine trace,
                                     EntityManager em) {
        this.projets = projets;
        this.droits = droits;
        this.trace = trace;
        this.em = em;
    }

    public PrevisionsChantierDto lire(UUID id) {
        PrjProject p = charger(id);
        droits.exiger(DROIT);
        return dto(p, types());
    }

    /** Toutes les validations avant la première écriture ; lignes existantes reprises par type de travaux. */
    @Transactional
    public PrevisionsChantierDto modifier(UUID id, PrevisionsChantierRequest saisie) {
        PrjProject p = charger(id);
        droits.exiger(DROIT);
        if (p.getStkLevelingId() == null) {
            throw new ConflictException("Le projet n'a pas de nivellement : prévisions chantier non modifiables.");
        }
        Map<UUID, GrhWorkedDayType> types = types();
        List<LignePrevisionChantierSaisie> lignes = valider(saisie, types.keySet());
        Map<UUID, PrjProjectBuildingPrevision> existantes = new HashMap<>();
        lignesDe(p.getId()).forEach(l -> {
            if (l.getGrhWorkedDayTypeId() == null || existantes.putIfAbsent(l.getGrhWorkedDayTypeId(), l) != null) {
                em.remove(l);
            }
        });
        for (LignePrevisionChantierSaisie s : lignes) {
            PrjProjectBuildingPrevision l = existantes.remove(s.typeTravauxId());
            if (l == null) {
                l = new PrjProjectBuildingPrevision();
                l.setId(UUID.randomUUID());
                l.setPrjProjectId(p.getId());
                l.setGrhWorkedDayTypeId(s.typeTravauxId());
                em.persist(l);
            }
            l.setQuantity(s.quantite());
            l.setUnitPriceHT(s.prixUnitaireHT());
            l.setVatRatio(MontantsLot.fraction(s.tva()));
            l.setNotes(s.notes());
            trace.horodater(l);
        }
        existantes.values().forEach(em::remove);
        em.flush();
        return dto(p, types);
    }

    private static List<LignePrevisionChantierSaisie> valider(PrevisionsChantierRequest saisie, Set<UUID> types) {
        if (saisie == null || saisie.lignes() == null || saisie.lignes().contains(null)) {
            throw new InvalidRequestException("lignes : la grille est obligatoire (liste vide pour tout supprimer).");
        }
        Set<UUID> vus = new HashSet<>();
        for (LignePrevisionChantierSaisie s : saisie.lignes()) {
            if (s.typeTravauxId() == null) {
                throw new InvalidRequestException("typeTravauxId : le type de travaux est obligatoire.");
            }
            if (!types.contains(s.typeTravauxId())) {
                throw new InvalidRequestException("typeTravauxId : type de travaux inconnu.");
            }
            if (!vus.add(s.typeTravauxId())) {
                throw new InvalidRequestException("typeTravauxId : un type de travaux ne figure qu'une fois dans la grille.");
            }
            borne(s.quantite(), "La quantité");
            borne(s.prixUnitaireHT(), "Le prix unitaire HT");
            borne(MontantsLot.fraction(s.tva()), "La TVA");
            if (s.notes() != null && s.notes().length() > NOTES_MAX) {
                throw new InvalidRequestException("notes : 1000 caractères au plus.");
            }
        }
        return saisie.lignes();
    }

    private static void borne(BigDecimal valeur, String libelle) {
        positif(valeur, libelle);
        if (valeur != null && valeur.precision() - valeur.scale() > CHIFFRES_ENTIERS) {
            throw new InvalidRequestException(libelle + " : valeur trop grande.");
        }
    }

    private PrevisionsChantierDto dto(PrjProject p, Map<UUID, GrhWorkedDayType> types) {
        boolean actif = p.getStkLevelingId() != null;
        List<LignePrevisionChantierDto> lignes = lignesDe(p.getId()).stream()
                .map(l -> ligneDto(l, types.get(l.getGrhWorkedDayTypeId())))
                .sorted(Comparator.comparing(LignePrevisionChantierDto::code, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        // droit déjà exigé pour lire : modifiable = actif
        return new PrevisionsChantierDto(p.getId(), p.getCode(), p.getDescription(), actif, actif, lignes,
                types.values().stream().map(t -> new TypeTravauxDto(t.getId(), t.getCode(), t.getDescription(), t.getUnity(),
                        t.getUnitPriceHT(), MontantsLot.pourcentage(t.getVatRatio()))).toList());
    }

    private static LignePrevisionChantierDto ligneDto(PrjProjectBuildingPrevision l, GrhWorkedDayType t) {
        BigDecimal ht = z(l.getUnitPriceHT()).multiply(z(l.getQuantity()));
        return new LignePrevisionChantierDto(l.getId(), l.getGrhWorkedDayTypeId(), t == null ? null : t.getCode(),
                t == null ? null : t.getDescription(), t == null ? null : t.getUnity(), l.getQuantity(), l.getUnitPriceHT(),
                ht, MontantsLot.pourcentage(l.getVatRatio()), ht.multiply(BigDecimal.ONE.add(z(l.getVatRatio()))), l.getNotes());
    }

    private static BigDecimal z(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
    }

    /** Types de travaux par code. */
    private Map<UUID, GrhWorkedDayType> types() {
        return em.createQuery("SELECT t FROM GrhWorkedDayType t ORDER BY t.code ASC", GrhWorkedDayType.class).getResultStream()
                .collect(Collectors.toMap(GrhWorkedDayType::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private List<PrjProjectBuildingPrevision> lignesDe(UUID projet) {
        return em.createQuery("SELECT l FROM PrjProjectBuildingPrevision l WHERE l.prjProjectId = :projet",
                PrjProjectBuildingPrevision.class).setParameter("projet", projet).getResultList();
    }

    private PrjProject charger(UUID id) {
        return projets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Projet introuvable: " + id));
    }
}
