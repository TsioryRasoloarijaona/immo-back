package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.planification.ContrainteTache;
import com.tsix.immocrm.domain.planification.NaturePlanification;
import com.tsix.immocrm.domain.planification.TexteGantt;
import com.tsix.immocrm.domain.planification.TypeDependance;
import com.tsix.immocrm.dto.projet.IntervenantPlanificationDto;
import com.tsix.immocrm.dto.projet.PlanificationDetailDto;
import com.tsix.immocrm.dto.projet.PlanificationListItemDto;
import com.tsix.immocrm.dto.projet.PredecesseurDto;
import com.tsix.immocrm.dto.projet.TachePlanificationDto;
import com.tsix.immocrm.entities.cfg.CfgGanttOwner;
import com.tsix.immocrm.entities.cfg.CfgGanttProject;
import com.tsix.immocrm.entities.cfg.CfgGanttTask;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Seule lecture d'une planification par id : projet relu sur la ligne stockée ({@code ObjectId}, ou celui de la planification
 * principale pour une version), hors périmètre = 404 ; droits et lecture seule des versions (FEATURE-n9-planification.md D5, D6, R13).
 */
@Component
public class AccesPlanification {

    static final String DROIT_GANTT = "Yes.Cfg.Front.Gantt.GanttProject";
    static final String DROIT_VERSION = "Yes.Prj.Front.Tdb.ProjectPrevision.toolSaveVersion";
    static final String ABSENTE = "PLANIFICATION_ABSENTE";
    static final String MESSAGE_ABSENTE = "Aucune planification n'est définie pour le projet en cours !";
    private static final String ID = "id";

    private final EntityManager em;
    private final PerimetreProjetService perimetre;
    private final DroitsOmran droits;

    public AccesPlanification(EntityManager em, PerimetreProjetService perimetre, DroitsOmran droits) {
        this.em = em;
        this.perimetre = perimetre;
        this.droits = droits;
    }

    CfgGanttProject lisible(UUID id) {
        CfgGanttProject p = em.find(CfgGanttProject.class, id);
        UUID projet = p == null ? null : projetDe(p);
        if (p == null || (projet != null && !perimetre.estVisible(projet))) {
            throw new ResourceNotFoundException("Planification introuvable : " + id);
        }
        return p;
    }

    /** 404 → 403 → 409 (une version est en lecture seule). */
    CfgGanttProject modifiable(UUID id) {
        CfgGanttProject p = lisible(id);
        droits.exiger(DROIT_GANTT);
        if (p.getVersionDate() != null) {
            throw new ConflictException("Une version de planification est en lecture seule.");
        }
        return p;
    }

    void exiger(String droit) {
        droits.exiger(droit);
    }

    PrjProject projetVisible(UUID projetId) {
        PrjProject projet = perimetre.estVisible(projetId) ? em.find(PrjProject.class, projetId) : null;
        if (projet == null) {
            throw new ResourceNotFoundException("Projet introuvable : " + projetId);
        }
        return projet;
    }

    Optional<CfgGanttProject> principale(UUID projetId) {
        return em.createQuery("""
                        SELECT p FROM CfgGanttProject p WHERE p.objectId = :projet AND p.versionDate IS NULL ORDER BY p.id ASC""",
                        CfgGanttProject.class)
                .setParameter("projet", projetId).setMaxResults(1).getResultStream().findFirst();
    }

    static ResourceNotFoundException absente() {
        return new ResourceNotFoundException(MESSAGE_ABSENTE, ABSENTE);
    }

    static NaturePlanification nature(CfgGanttProject p) {
        if (p.getVersionDate() != null) {
            return NaturePlanification.VERSION;
        }
        return p.getObjectId() == null ? NaturePlanification.MODELE : NaturePlanification.PROJET;
    }

    private UUID projetDe(CfgGanttProject p) {
        if (p.getObjectId() != null || p.getMainVersionId() == null) {
            return p.getObjectId();
        }
        CfgGanttProject principale = em.find(CfgGanttProject.class, p.getMainVersionId());
        return principale == null ? null : principale.getObjectId();
    }

    // ------------------------------------------------------------------ lectures

    List<CfgGanttTask> taches(UUID planification) {
        return em.createQuery("""
                        SELECT t FROM CfgGanttTask t WHERE t.cfgGanttProjectId = :id ORDER BY t.sorting ASC, t.code ASC""",
                        CfgGanttTask.class)
                .setParameter(ID, planification).getResultList();
    }

    List<CfgGanttOwner> intervenants(UUID planification) {
        return em.createQuery("SELECT o FROM CfgGanttOwner o WHERE o.cfgGanttProjectId = :id ORDER BY o.code ASC",
                        CfgGanttOwner.class)
                .setParameter(ID, planification).getResultList();
    }

    List<PlanificationListItemDto> versions(UUID planification) {
        return em.createQuery("""
                        SELECT p FROM CfgGanttProject p WHERE p.mainVersionId = :id AND p.versionDate IS NOT NULL
                        ORDER BY p.versionDate DESC""", CfgGanttProject.class)
                .setParameter(ID, planification).getResultStream().map(AccesPlanification::ligne).toList();
    }

    PlanificationDetailDto detail(CfgGanttProject p) {
        em.flush();
        List<PlanificationListItemDto> versions = p.getVersionDate() == null ? versions(p.getId()) : List.of();
        return new PlanificationDetailDto(p.getId(), p.getCode(), p.getDescription(), p.getNotes(), jour(p.getStartDate()),
                p.getVersionDate(), nature(p), projetDe(p), p.getMainVersionId(), dtos(taches(p.getId())),
                intervenants(p.getId()).stream().map(AccesPlanification::intervenant).toList(), versions);
    }

    /** Prédécesseurs lus avec les rangs de toute la planification : passer toutes ses tâches. */
    static List<TachePlanificationDto> dtos(List<CfgGanttTask> taches) {
        Map<Integer, UUID> parRang = new HashMap<>();
        taches.stream().filter(t -> t.getSorting() != null).forEach(t -> parRang.putIfAbsent(t.getSorting(), t.getId()));
        return taches.stream().map(t -> tache(t, parRang)).toList();
    }

    static PlanificationListItemDto ligne(CfgGanttProject p) {
        return new PlanificationListItemDto(p.getId(), p.getCode(), p.getDescription(), p.getNotes(), jour(p.getStartDate()),
                p.getVersionDate());
    }

    private static TachePlanificationDto tache(CfgGanttTask t, Map<Integer, UUID> parRang) {
        LocalDate debut = jour(t.getStartDate());
        LocalDate fin = debut == null || t.getDuration() == null ? null : debut.plusDays(t.getDuration());
        List<PredecesseurDto> predecesseurs = TexteGantt.lirePredecesseurs(t.getPredecessors(), parRang).stream()
                .map(l -> new PredecesseurDto(l.tache(), TypeDependance.depuis(l.type()))).toList();
        return new TachePlanificationDto(t.getId(), t.getParentId(), t.getCode(), t.getDescription(), t.getNotes(),
                t.getSorting(), t.getDepth(), debut, t.getDuration(), fin, t.getPercentComplete(), t.getAlertBeforeDays(),
                ContrainteTache.depuis(t.getConstraintType()), jour(t.getConstraintDate()), jour(t.getDeadline()),
                t.getIsMilestone(), predecesseurs, TexteGantt.lireIntervenants(t.getResourceNames()), t.getCfgGanttTaskTypeId(),
                t.getPrjMarketId(), t.getStlCategoryId());
    }

    private static IntervenantPlanificationDto intervenant(CfgGanttOwner o) {
        return new IntervenantPlanificationDto(o.getId(), o.getCode(), o.getDescription(), o.getNotes(), o.getVisible(),
                o.getCfgTierId());
    }

    private static LocalDate jour(LocalDateTime date) {
        return date == null ? null : date.toLocalDate();
    }
}
