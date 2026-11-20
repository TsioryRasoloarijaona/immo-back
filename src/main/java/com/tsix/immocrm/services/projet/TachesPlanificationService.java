package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.planification.ContrainteTache;
import com.tsix.immocrm.domain.planification.TexteGantt;
import com.tsix.immocrm.domain.planification.TypeDependance;
import com.tsix.immocrm.dto.projet.IntervenantPlanificationDto;
import com.tsix.immocrm.dto.projet.PlanificationDetailDto;
import com.tsix.immocrm.dto.projet.PredecesseurDto;
import com.tsix.immocrm.dto.projet.TacheEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgGanttOwner;
import com.tsix.immocrm.entities.cfg.CfgGanttProject;
import com.tsix.immocrm.entities.cfg.CfgGanttTask;
import com.tsix.immocrm.entities.cfg.CfgGanttTaskType;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Tâches, intervenants et date de début d'une planification (FEATURE-n9-planification.md R9-R12). */
@Service
@Transactional
public class TachesPlanificationService {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private final EntityManager em;
    private final AccesPlanification acces;
    private final TracePatrimoine trace;

    public TachesPlanificationService(EntityManager em, AccesPlanification acces, TracePatrimoine trace) {
        this.em = em;
        this.acces = acces;
        this.trace = trace;
    }

    // ------------------------------------------------------------------ tâches

    /** R9 ({@code GanttEditor.cs:AddRootTask / AddChildTask}) : valeurs par défaut, puis les champs saisis. */
    public PlanificationDetailDto creerTache(UUID id, TacheEcritureDto corps) {
        CfgGanttProject p = acces.modifiable(id);
        TachesEditees taches = new TachesEditees(acces.taches(id));
        CfgGanttTask parent = corps.parentId() == null || Boolean.TRUE.equals(corps.racine()) ? null : parent(taches, corps.parentId());
        valider(p, taches, null, corps);
        UUID parentId = parent == null ? null : parent.getId();
        int rang = taches.enfants(parentId).size() + 1;
        CfgGanttTask t = new CfgGanttTask();
        t.setId(UUID.randomUUID());
        t.setCfgGanttProjectId(id);
        t.setParentId(parentId);
        t.setCode(parent == null ? String.valueOf(rang) : parent.getCode() + "." + rang);
        t.setDescription((parent == null ? "Tâche " : "Sous-tâche ") + t.getCode());
        t.setDuration(parent == null ? Integer.valueOf(1) : parent.getDuration());
        t.setStartDate(parent == null ? p.getStartDate() : parent.getStartDate());
        t.setPercentComplete(BigDecimal.ZERO);
        t.setConstraintType(ContrainteTache.AU_PLUS_TOT.valeur());
        taches.ajouter(t, taches.cleSuivante(parentId));
        appliquer(taches, t, corps);
        em.persist(t);
        taches.renumeroter();
        return acces.detail(p);
    }

    public PlanificationDetailDto modifierTache(UUID id, UUID tacheId, TacheEcritureDto corps) {
        CfgGanttProject p = acces.modifiable(id);
        TachesEditees taches = new TachesEditees(acces.taches(id));
        CfgGanttTask t = taches.tache(tacheId);
        valider(p, taches, tacheId, corps);
        if (Boolean.TRUE.equals(corps.racine())) {
            if (t.getParentId() != null) {
                deplacer(taches, t, null);
            }
        } else if (corps.parentId() != null && !corps.parentId().equals(t.getParentId())) {
            deplacer(taches, t, corps.parentId());
        }
        appliquer(taches, t, corps);
        taches.renumeroter();
        return acces.detail(p);
    }

    public PlanificationDetailDto supprimerTache(UUID id, UUID tacheId) {
        CfgGanttProject p = acces.modifiable(id);
        TachesEditees taches = new TachesEditees(acces.taches(id));
        taches.tache(tacheId);
        List<UUID> branche = taches.branche(tacheId);
        branche.forEach(b -> em.remove(taches.tache(b)));
        taches.retirer(branche);
        taches.renumeroter();
        return acces.detail(p);
    }

    /** Nouvel ordre des sœurs d'un même parent : la liste doit les contenir toutes, une fois chacune. */
    public PlanificationDetailDto ordonnerTaches(UUID id, List<UUID> ids) {
        CfgGanttProject p = acces.modifiable(id);
        TachesEditees taches = new TachesEditees(acces.taches(id));
        if (ids == null || ids.isEmpty() || !ids.stream().allMatch(taches::contient)) {
            throw new InvalidRequestException("La liste doit citer des tâches de cette planification.");
        }
        Set<UUID> soeurs = new HashSet<>(taches.enfants(taches.tache(ids.get(0)).getParentId()).stream()
                .map(CfgGanttTask::getId).toList());
        if (ids.size() != soeurs.size() || !soeurs.equals(new HashSet<>(ids))) {
            throw new InvalidRequestException("La liste doit citer toutes les sœurs d'un même parent, une fois chacune.");
        }
        for (int i = 0; i < ids.size(); i++) {
            taches.cle(ids.get(i), i + 1d);
        }
        taches.renumeroter();
        return acces.detail(p);
    }

    /** R12 ({@code GanttEditor.cs:ModifyStartDate}) : débuts et contraintes décalés ; échéance seulement si après le nouveau début. */
    public PlanificationDetailDto modifierDateDebut(UUID id, LocalDate dateDebut) {
        CfgGanttProject p = acces.modifiable(id);
        if (dateDebut == null) {
            throw new InvalidRequestException("La date de début est obligatoire.");
        }
        LocalDateTime nouvelle = dateDebut.atStartOfDay();
        long ecart = p.getStartDate() == null ? 0 : ChronoUnit.DAYS.between(p.getStartDate().toLocalDate(), dateDebut);
        p.setStartDate(nouvelle);
        trace.horodater(p);
        if (ecart != 0) {
            acces.taches(id).forEach(t -> {
                t.setStartDate(decaler(t.getStartDate(), ecart));
                t.setConstraintDate(decaler(t.getConstraintDate(), ecart));
                if (t.getDeadline() != null && t.getDeadline().isAfter(nouvelle)) {
                    t.setDeadline(t.getDeadline().plusDays(ecart));
                }
            });
        }
        return acces.detail(p);
    }

    // ------------------------------------------------------------------ intervenants

    public PlanificationDetailDto creerIntervenant(UUID id, IntervenantPlanificationDto corps) {
        CfgGanttProject p = acces.modifiable(id);
        validerIntervenant(corps, true);
        CfgGanttOwner o = new CfgGanttOwner();
        o.setId(UUID.randomUUID());
        o.setCfgGanttProjectId(id);
        o.setVisible(true);
        appliquer(o, corps);
        em.persist(o);
        return acces.detail(p);
    }

    public PlanificationDetailDto modifierIntervenant(UUID id, UUID intervenantId, IntervenantPlanificationDto corps) {
        CfgGanttProject p = acces.modifiable(id);
        CfgGanttOwner o = intervenant(id, intervenantId);
        validerIntervenant(corps, false);
        appliquer(o, corps);
        return acces.detail(p);
    }

    /** R11 : l'intervenant est aussi retiré des {@code ResourceNames} des tâches. */
    public PlanificationDetailDto supprimerIntervenant(UUID id, UUID intervenantId) {
        CfgGanttProject p = acces.modifiable(id);
        CfgGanttOwner o = intervenant(id, intervenantId);
        acces.taches(id).forEach(t -> {
            List<UUID> ressources = TexteGantt.lireIntervenants(t.getResourceNames());
            if (ressources.contains(intervenantId)) {
                t.setResourceNames(TexteGantt.ecrireIntervenants(ressources.stream()
                        .filter(r -> !r.equals(intervenantId)).toList()));
            }
        });
        em.remove(o);
        return acces.detail(p);
    }

    // ------------------------------------------------------------------ validation (avant tout setter)

    private void valider(CfgGanttProject p, TachesEditees taches, UUID tacheId, TacheEcritureDto c) {
        if (tacheId != null && c.parentId() != null && !Boolean.TRUE.equals(c.racine())) {
            parent(taches, c.parentId());
            if (taches.dansLaBranche(tacheId, c.parentId())) {
                throw new InvalidRequestException("Le parent choisi est la tâche elle-même ou l'un de ses descendants.");
            }
        }
        if (c.code() != null) {
            PlanificationService.texte(c.code(), "Le code");
        }
        if (c.description() != null) {
            PlanificationService.texte(c.description(), "La description");
        }
        PlanificationService.notes(c.notes());
        positif(c.duree(), "La durée");
        positif(c.alerteAvantJours(), "L'alerte");
        if (c.avancement() != null && (c.avancement().signum() < 0 || c.avancement().compareTo(CENT) > 0)) {
            throw new InvalidRequestException("L'avancement est compris entre 0 et 100.");
        }
        if (c.typeTacheId() != null && em.find(CfgGanttTaskType.class, c.typeTacheId()) == null) {
            throw new InvalidRequestException("Type de tâche inconnu : " + c.typeTacheId());
        }
        validerPredecesseurs(taches, tacheId, c.predecesseurs());
        validerIntervenants(p, c.intervenants());
    }

    private static void validerPredecesseurs(TachesEditees taches, UUID tacheId, List<PredecesseurDto> predecesseurs) {
        if (predecesseurs == null) {
            return;
        }
        for (PredecesseurDto l : predecesseurs) {
            if (l == null || !taches.contient(l.tacheId()) || l.tacheId().equals(tacheId)) {
                throw new InvalidRequestException("Prédécesseur inconnu ou égal à la tâche : " + (l == null ? null : l.tacheId()));
            }
        }
    }

    private void validerIntervenants(CfgGanttProject p, List<UUID> intervenants) {
        if (intervenants == null) {
            return;
        }
        Set<UUID> propres = new HashSet<>(acces.intervenants(p.getId()).stream().map(CfgGanttOwner::getId).toList());
        if (!propres.containsAll(intervenants)) {
            throw new InvalidRequestException("Un intervenant n'appartient pas à cette planification.");
        }
    }

    private void validerIntervenant(IntervenantPlanificationDto c, boolean creation) {
        if (creation || c.code() != null) {
            PlanificationService.texte(c.code(), "Le code");
        }
        if (c.description() != null) {
            PlanificationService.texte(c.description(), "La description");
        }
        PlanificationService.notes(c.notes());
        if (c.tiersId() != null && em.find(CfgTier.class, c.tiersId()) == null) {
            throw new InvalidRequestException("Tiers inconnu : " + c.tiersId());
        }
    }

    private static CfgGanttTask parent(TachesEditees taches, UUID parentId) {
        if (!taches.contient(parentId)) {
            throw new InvalidRequestException("Le parent doit être une tâche de cette planification : " + parentId);
        }
        return taches.tache(parentId);
    }

    private static void positif(Integer valeur, String libelle) {
        if (valeur != null && valeur < 0) {
            throw new InvalidRequestException(libelle + " est un nombre de jours positif ou nul.");
        }
    }

    // ------------------------------------------------------------------ écriture

    private static void deplacer(TachesEditees taches, CfgGanttTask t, UUID parentId) {
        double cle = taches.cleSuivante(parentId);
        t.setParentId(parentId);
        taches.cle(t.getId(), cle);
    }

    private void appliquer(TachesEditees taches, CfgGanttTask t, TacheEcritureDto c) {
        if (c.code() != null) {
            t.setCode(c.code().trim());
        }
        if (c.description() != null) {
            t.setDescription(c.description().trim());
        }
        if (c.notes() != null) {
            t.setNotes(c.notes());
        }
        if (c.dateDebut() != null) {
            t.setStartDate(c.dateDebut().atStartOfDay());
        }
        if (c.duree() != null) {
            t.setDuration(c.duree());
        }
        if (c.avancement() != null) {
            t.setPercentComplete(c.avancement());
        }
        if (c.alerteAvantJours() != null) {
            t.setAlertBeforeDays(c.alerteAvantJours());
        }
        if (c.contrainte() != null) {
            t.setConstraintType(c.contrainte().valeur());
        }
        if (c.dateContrainte() != null) {
            t.setConstraintDate(c.dateContrainte().atStartOfDay());
        }
        if (c.echeance() != null) {
            t.setDeadline(c.echeance().atStartOfDay());
        }
        if (c.jalon() != null) {
            t.setIsMilestone(c.jalon());
        }
        if (c.typeTacheId() != null) {
            t.setCfgGanttTaskTypeId(c.typeTacheId());
        }
        if (c.predecesseurs() != null) {
            taches.liens(t.getId(), c.predecesseurs().stream().map(l -> new TexteGantt.Lien(l.tacheId(),
                    Objects.requireNonNullElse(l.type(), TypeDependance.FIN_DEBUT).valeur())).toList());
        }
        if (c.intervenants() != null) {
            t.setResourceNames(TexteGantt.ecrireIntervenants(c.intervenants().stream().distinct().toList()));
        }
        trace.horodater(t);
    }

    private void appliquer(CfgGanttOwner o, IntervenantPlanificationDto c) {
        if (c.code() != null) {
            o.setCode(c.code().trim());
        }
        if (c.description() != null) {
            o.setDescription(c.description());
        }
        if (c.notes() != null) {
            o.setNotes(c.notes());
        }
        if (c.visible() != null) {
            o.setVisible(c.visible());
        }
        if (c.tiersId() != null) {
            o.setCfgTierId(c.tiersId());
        }
        trace.horodater(o);
    }

    private CfgGanttOwner intervenant(UUID planification, UUID intervenantId) {
        CfgGanttOwner o = em.find(CfgGanttOwner.class, intervenantId);
        if (o == null || !planification.equals(o.getCfgGanttProjectId())) {
            throw new ResourceNotFoundException("Intervenant introuvable : " + intervenantId);
        }
        return o;
    }

    private static LocalDateTime decaler(LocalDateTime date, long jours) {
        return date == null ? null : date.plusDays(jours);
    }
}
