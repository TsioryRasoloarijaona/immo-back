package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.planification.NaturePlanification;
import com.tsix.immocrm.dto.projet.PlanificationDetailDto;
import com.tsix.immocrm.dto.projet.PlanificationEcritureDto;
import com.tsix.immocrm.dto.projet.PlanificationListItemDto;
import com.tsix.immocrm.dto.projet.TachePlanificationDto;
import com.tsix.immocrm.dto.projet.VersionSauvegardeeDto;
import com.tsix.immocrm.entities.cfg.CfgGanttProject;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Modèles, planification d'un projet, clonage et versions (FEATURE-n9-planification.md R5-R8, R13). */
@Service
@Transactional
public class PlanificationService {

    private static final int LONGUEUR = 255;
    private static final int LONGUEUR_NOTES = 1000;
    private static final String IDS = "ids";

    private final EntityManager em;
    private final AccesPlanification acces;
    private final ClonagePlanification clonage;
    private final LotsProjetService lots;
    private final TracePatrimoine trace;
    private final Clock clock;

    public PlanificationService(EntityManager em, AccesPlanification acces, ClonagePlanification clonage,
                                LotsProjetService lots, TracePatrimoine trace, Clock clock) {
        this.em = em;
        this.acces = acces;
        this.clonage = clonage;
        this.lots = lots;
        this.trace = trace;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PlanificationListItemDto> listerModeles() {
        return em.createQuery(
                        "SELECT p FROM CfgGanttProject p WHERE p.objectId IS NULL AND p.versionDate IS NULL ORDER BY p.code ASC",
                        CfgGanttProject.class)
                .getResultStream().map(AccesPlanification::ligne).toList();
    }

    @Transactional(readOnly = true)
    public PlanificationDetailDto lire(UUID id) {
        return acces.detail(acces.lisible(id));
    }

    @Transactional(readOnly = true)
    public List<PlanificationListItemDto> listerVersions(UUID id) {
        return acces.versions(acces.lisible(id).getId());
    }

    public PlanificationDetailDto creerModele(PlanificationEcritureDto corps) {
        acces.exiger(AccesPlanification.DROIT_GANTT);
        String code = texte(corps.code(), "Le code");
        String description = texte(corps.description(), "La description");
        String notes = notes(corps.notes());
        clonage.verifierCodeLibre(code, null);
        CfgGanttProject m = new CfgGanttProject();
        m.setId(UUID.randomUUID());
        m.setCode(code);
        m.setDescription(description);
        m.setNotes(notes);
        m.setStartDate(Objects.requireNonNullElseGet(corps.dateDebut(), () -> LocalDate.now(clock)).atStartOfDay());
        trace.horodater(m);
        em.persist(m);
        return acces.detail(m);
    }

    public PlanificationDetailDto modifierModele(UUID id, PlanificationEcritureDto corps) {
        CfgGanttProject m = modele(id);
        acces.exiger(AccesPlanification.DROIT_GANTT);
        String code = corps.code() == null ? null : texte(corps.code(), "Le code");
        String description = corps.description() == null ? null : texte(corps.description(), "La description");
        String notes = notes(corps.notes());
        if (code != null) {
            clonage.verifierCodeLibre(code, id);
            m.setCode(code);
        }
        if (description != null) {
            m.setDescription(description);
        }
        if (notes != null) {
            m.setNotes(notes);
        }
        if (corps.dateDebut() != null) {
            m.setStartDate(corps.dateDebut().atStartOfDay());
        }
        trace.horodater(m);
        return acces.detail(m);
    }

    /** {@code CfgGanttProject.cs:BeforeDelete} : versions d'abord ; les enfants sont retirés ici, sans compter sur une FK (D7). */
    public void supprimerModele(UUID id) {
        CfgGanttProject m = modele(id);
        acces.exiger(AccesPlanification.DROIT_GANTT);
        List<UUID> ids = new ArrayList<>(em.createQuery(
                        "SELECT p.id FROM CfgGanttProject p WHERE p.mainVersionId = :id", UUID.class)
                .setParameter("id", id).getResultList());
        List<UUID> versions = List.copyOf(ids);
        ids.add(id);
        em.createQuery("DELETE FROM CfgGanttTask t WHERE t.cfgGanttProjectId IN :ids").setParameter(IDS, ids).executeUpdate();
        em.createQuery("DELETE FROM CfgGanttOwner o WHERE o.cfgGanttProjectId IN :ids").setParameter(IDS, ids).executeUpdate();
        if (!versions.isEmpty()) {
            em.createQuery("DELETE FROM CfgGanttProject p WHERE p.id IN :ids").setParameter(IDS, versions).executeUpdate();
        }
        em.remove(m);
        em.flush();
    }

    /** R13 : planification d'un projet seulement ({@code Clone(null, null, maintenant)}). */
    public VersionSauvegardeeDto sauvegarderVersion(UUID id) {
        CfgGanttProject p = acces.lisible(id);
        acces.exiger(AccesPlanification.DROIT_VERSION);
        if (AccesPlanification.nature(p) != NaturePlanification.PROJET) {
            throw new ConflictException("Seule la planification d'un projet peut être sauvegardée en version.");
        }
        CfgGanttProject v = clonage.cloner(p, null, LocalDateTime.now(clock).withNano(0));
        return new VersionSauvegardeeDto(AccesPlanification.ligne(v),
                "Version '%s' sauvegardée correctement.".formatted(v.getDescription()));
    }

    @Transactional(readOnly = true)
    public PlanificationDetailDto lireDuProjet(UUID projetId) {
        acces.projetVisible(projetId);
        return acces.detail(acces.principale(projetId).orElseThrow(AccesPlanification::absente));
    }

    /** R7 / R8 : au plus une planification par projet ; le modèle doit en être un (400 sinon). */
    public PlanificationDetailDto clonerVersProjet(UUID projetId, UUID modeleId) {
        PrjProject projet = acces.projetVisible(projetId);
        acces.exiger(AccesPlanification.DROIT_GANTT);
        if (acces.principale(projetId).isPresent()) {
            throw new ConflictException("Ce projet a déjà une planification.");
        }
        CfgGanttProject modele = modeleId == null ? null : em.find(CfgGanttProject.class, modeleId);
        if (modele == null || AccesPlanification.nature(modele) != NaturePlanification.MODELE) {
            throw new InvalidRequestException("Modèle de planification inconnu : " + modeleId);
        }
        return acces.detail(clonage.cloner(modele, projet, null));
    }

    @Transactional(readOnly = true)
    public List<TachePlanificationDto> tachesDuLot(UUID lotId) {
        CfgGanttProject p = acces.principale(lots.visible(lotId).getPrjProject().getId())
                .orElseThrow(AccesPlanification::absente);
        return AccesPlanification.dtos(acces.taches(p.getId())).stream().filter(t -> lotId.equals(t.lotId())).toList();
    }

    private CfgGanttProject modele(UUID id) {
        CfgGanttProject m = acces.lisible(id);
        if (AccesPlanification.nature(m) != NaturePlanification.MODELE) {
            throw new ResourceNotFoundException("Modèle de planification introuvable : " + id);
        }
        return m;
    }

    static String texte(String valeur, String libelle) {
        String t = valeur == null ? "" : valeur.trim();
        if (t.isEmpty() || t.length() > LONGUEUR) {
            throw new InvalidRequestException("%s est obligatoire (%d caractères au plus).".formatted(libelle, LONGUEUR));
        }
        return t;
    }

    static String notes(String notes) {
        if (notes != null && notes.length() > LONGUEUR_NOTES) {
            throw new InvalidRequestException("Les notes font %d caractères au plus.".formatted(LONGUEUR_NOTES));
        }
        return notes;
    }
}
