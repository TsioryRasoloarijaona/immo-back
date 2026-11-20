package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.planification.TexteGantt;
import com.tsix.immocrm.entities.cfg.CfgGanttOwner;
import com.tsix.immocrm.entities.cfg.CfgGanttProject;
import com.tsix.immocrm.entities.cfg.CfgGanttTask;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** {@code CfgGanttProject.cs:Clone} : copie d'une planification vers un projet ou en version datée (FEATURE-n9-planification.md R8, R13). */
@Component
public class ClonagePlanification {

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EntityManager em;
    private final TracePatrimoine trace;
    private final Clock clock;

    public ClonagePlanification(EntityManager em, TracePatrimoine trace, Clock clock) {
        this.em = em;
        this.trace = trace;
        this.clock = clock;
    }

    /** {@code projet} null = version datée de {@code source} ; sinon planification principale du projet. */
    CfgGanttProject cloner(CfgGanttProject source, PrjProject projet, LocalDateTime dateVersion) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        String code = (projet == null ? source.getCode() : projet.getCode()) + " - "
                + (dateVersion == null ? maintenant : dateVersion).format(DATE_HEURE);
        verifierCodeLibre(code, null);
        CfgGanttProject copie = new CfgGanttProject();
        copie.setId(UUID.randomUUID());
        copie.setCode(code);
        copie.setDescription(dateVersion == null ? "Version principale" : dateVersion.format(DATE));
        copie.setMainVersionId(dateVersion == null ? null : source.getId());
        copie.setNotes(source.getNotes());
        copie.setStartDate(LocalDate.now(clock).atStartOfDay());
        copie.setVersionDate(dateVersion);
        if (projet != null) {
            copie.setObjectId(projet.getId());
            copie.setStrEntityObjectId(projet.getStrEntity() == null ? null : projet.getStrEntity().getId());
        }
        trace.horodater(copie);
        em.persist(copie);
        Map<UUID, UUID> intervenants = copierIntervenants(source.getId(), copie.getId());
        copierTaches(source.getId(), copie.getId(), intervenants, lotsParCategorie(projet));
        return copie;
    }

    /** Code unique parmi toutes les planifications (modèles, projets, versions) ; {@code id} = la ligne modifiée. */
    void verifierCodeLibre(String code, UUID id) {
        ReferentielAdminService.verrouiller(em, CfgGanttProject.class, code, "Planification");
        long pris = em.createQuery("""
                        SELECT COUNT(p) FROM CfgGanttProject p WHERE LOWER(TRIM(p.code)) = LOWER(:code) AND p.id <> :id""",
                        Long.class)
                .setParameter("code", code.trim()).setParameter("id", id == null ? new UUID(0, 0) : id).getSingleResult();
        if (pris > 0) {
            throw new ConflictException("Le code %s est déjà pris par une autre planification.".formatted(code));
        }
    }

    private Map<UUID, UUID> copierIntervenants(UUID source, UUID copie) {
        Map<UUID, UUID> nouveaux = new HashMap<>();
        em.createQuery("SELECT o FROM CfgGanttOwner o WHERE o.cfgGanttProjectId = :id", CfgGanttOwner.class)
                .setParameter("id", source).getResultList().forEach(o -> {
                    CfgGanttOwner c = new CfgGanttOwner();
                    c.setId(UUID.randomUUID());
                    c.setCode(o.getCode());
                    c.setDescription(o.getDescription());
                    c.setNotes(o.getNotes());
                    c.setCfgTierId(o.getCfgTierId());
                    c.setVisible(o.getVisible());
                    c.setCfgGanttProjectId(copie);
                    trace.horodater(c);
                    em.persist(c);
                    nouveaux.put(o.getId(), c.getId());
                });
        return nouveaux;
    }

    private void copierTaches(UUID source, UUID copie, Map<UUID, UUID> intervenants, Map<UUID, UUID> lots) {
        List<CfgGanttTask> taches = em.createQuery("SELECT t FROM CfgGanttTask t WHERE t.cfgGanttProjectId = :id",
                CfgGanttTask.class).setParameter("id", source).getResultList();
        Map<UUID, UUID> nouvelles = new HashMap<>();
        taches.forEach(t -> nouvelles.put(t.getId(), UUID.randomUUID()));
        taches.forEach(t -> {
            CfgGanttTask c = new CfgGanttTask();
            c.setId(nouvelles.get(t.getId()));
            c.setCfgGanttProjectId(copie);
            c.setCode(t.getCode());
            c.setDescription(t.getDescription());
            c.setNotes(t.getNotes());
            c.setSorting(t.getSorting());
            c.setDepth(t.getDepth());
            c.setStartDate(t.getStartDate());
            c.setDuration(t.getDuration());
            c.setPercentComplete(t.getPercentComplete());
            c.setAlertBeforeDays(t.getAlertBeforeDays());
            c.setConstraintType(t.getConstraintType());
            c.setConstraintDate(t.getConstraintDate());
            c.setDeadline(t.getDeadline());
            c.setPredecessors(t.getPredecessors());
            c.setIsMilestone(t.getIsMilestone());
            c.setCfgGanttTaskTypeId(t.getCfgGanttTaskTypeId());
            c.setStlCategoryId(t.getStlCategoryId());
            c.setPrjMarketId(t.getStlCategoryId() == null ? null : lots.get(t.getStlCategoryId()));
            c.setStrEntityLinkedId(t.getStrEntityLinkedId());
            c.setStrWorkFlowStartId(t.getStrWorkFlowStartId());
            c.setStrWorkFlowEndId(t.getStrWorkFlowEndId());
            c.setParentId(t.getParentId() == null ? null : nouvelles.get(t.getParentId()));
            c.setResourceNames(TexteGantt.ecrireIntervenants(TexteGantt.lireIntervenants(t.getResourceNames()).stream()
                    .map(intervenants::get).filter(Objects::nonNull).toList()));
            trace.horodater(c);
            em.persist(c);
        });
    }

    /** Premier lot du projet (par code) de chaque catégorie de dépense ; vide pour une version. */
    private Map<UUID, UUID> lotsParCategorie(PrjProject projet) {
        Map<UUID, UUID> lots = new HashMap<>();
        if (projet == null) {
            return lots;
        }
        List<?> lignes = em.createNativeQuery("""
                        SELECT CAST(StlCategoryId AS nvarchar(36)), CAST(PKey AS nvarchar(36)) FROM PrjMarket
                        WHERE PrjProjectId = :projet AND StlCategoryId IS NOT NULL ORDER BY Code ASC""")
                .setParameter("projet", projet.getId()).getResultList();
        lignes.forEach(l -> {
            Object[] ligne = (Object[]) l;
            lots.putIfAbsent(UUID.fromString((String) ligne[0]), UUID.fromString((String) ligne[1]));
        });
        return lots;
    }
}
