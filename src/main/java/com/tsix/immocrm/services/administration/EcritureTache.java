package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.TacheEcritureDto;
import com.tsix.immocrm.entities.adm.AdmModule;
import com.tsix.immocrm.entities.adm.AdmTask;
import com.tsix.immocrm.entities.adm.AdmTaskPriority;
import com.tsix.immocrm.entities.adm.AdmTaskType;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.SaisiePatrimoine;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;

/** Saisie d'une tâche : tous les contrôles passent avant le premier setter, un refus ne laisse rien à flusher. */
@Component
record EcritureTache(EntityManager em, TracePatrimoine trace) {

    private static final int NOTES_MAX = 1000;

    void appliquer(AdmTask t, TacheEcritureDto d) {
        String description = SaisiePatrimoine.obligatoire(d.description(), t.getDescription(), "description");
        String notes = d.notes() == null ? t.getNotes() : SaisiePatrimoine.texte(d.notes(), null, NOTES_MAX, "notes");
        AdmTaskType type = reference(AdmTaskType.class, d.typeId(), t.getType(), "typeId");
        AdmTaskPriority priorite = reference(AdmTaskPriority.class, d.prioriteId(), t.getPriorite(), "prioriteId");
        CfgTier declencheur = reference(CfgTier.class, d.declencheurId(), t.getDeclencheur(), "declencheurId");
        CfgTier gestionnaire = reference(CfgTier.class, d.gestionnaireId(), t.getGestionnaire(), "gestionnaireId");
        CfgTier assigne = reference(CfgTier.class, d.assigneId(), t.getAssigne(), "assigneId");
        AdmModule module = reference(AdmModule.class, d.moduleId(), t.getModule(), "moduleId");
        CfgCompany entite = reference(CfgCompany.class, d.entiteId(), t.getCfgCompany(), "entiteId");
        PrjProject projet = reference(PrjProject.class, d.projetId(), t.getPrjProject(), "projetId");

        t.setDescription(description);
        t.setNotes(notes);
        t.setType(type);
        t.setPriorite(priorite);
        t.setDeclencheur(declencheur);
        t.setGestionnaire(gestionnaire);
        t.setAssigne(assigne);
        t.setModule(module);
        t.setCfgCompany(entite);
        t.setPrjProject(projet);
        t.setStartDatePrev(ou(d.datePrevue(), t.getStartDatePrev()));
        t.setDelayPrev(ou(d.dureePrevue(), t.getDelayPrev()));
        t.setDueDatePrev(ou(d.dateFinPrevue(), t.getDueDatePrev()));
        t.setStartDate(ou(d.dateDebut(), t.getStartDate()));
        t.setDelay(ou(d.duree(), t.getDelay()));
        t.setEndDate(ou(d.dateFin(), t.getEndDate()));
    }

    void tracerCreation(AdmTask t) {
        TracePatrimoine.Trace maintenant = trace.maintenant();
        t.setUserCreate(maintenant.auteur());
        t.setUserCreateDateTime(maintenant.le());
    }

    void tracerModification(AdmTask t) {
        TracePatrimoine.Trace maintenant = trace.maintenant();
        t.setUserUpdate(maintenant.auteur());
        t.setUserUpdateDateTime(maintenant.le());
    }

    private <E> E reference(Class<E> classe, UUID id, E actuelle, String champ) {
        if (id == null) {
            return actuelle;
        }
        E trouvee = em.find(classe, id);
        if (trouvee == null) {
            throw new InvalidRequestException(champ + " : référence inconnue.");
        }
        return trouvee;
    }
}
