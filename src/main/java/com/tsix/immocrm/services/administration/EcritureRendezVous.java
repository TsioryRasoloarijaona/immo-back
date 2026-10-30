package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.RendezVousEcritureDto;
import com.tsix.immocrm.entities.adm.AdmTask;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.SaisiePatrimoine;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;

/** Saisie d'un rendez-vous : tous les contrôles passent avant le premier setter, comme {@link EcritureTache}. */
@Component
record EcritureRendezVous(EcritureTache tache) {

    void creer(AdmTask r, RendezVousEcritureDto d) {
        if (d.tierId() == null) {
            throw new InvalidRequestException("tierId est obligatoire.");
        }
        if (d.datePrevue() == null) {
            throw new InvalidRequestException("datePrevue est obligatoire.");
        }
        appliquer(r, d);
        r.setDueDatePrev(ou(d.dateFinPrevue(), d.datePrevue()));
        tache.tracerCreation(r);
    }

    void modifier(AdmTask r, RendezVousEcritureDto d) {
        appliquer(r, d);
        tache.tracerModification(r);
    }

    private void appliquer(AdmTask r, RendezVousEcritureDto d) {
        String description = SaisiePatrimoine.obligatoire(d.description(), r.getDescription(), "description");
        String notes = SaisiePatrimoine.texte(d.notes(), r.getNotes(), EcritureTache.NOTES_MAX, "notes");
        CfgTier declencheur = tache.reference(CfgTier.class, d.declencheurId(), r.getDeclencheur(), "declencheurId");
        CfgTier gestionnaire = tache.reference(CfgTier.class, d.gestionnaireId(), r.getGestionnaire(), "gestionnaireId");
        tache.reference(CfgTier.class, d.tierId(), null, "tierId");
        LocalDateTime debut = ou(d.datePrevue(), r.getStartDatePrev());
        LocalDateTime fin = ou(d.dateFinPrevue(), r.getDueDatePrev());
        if (debut != null && fin != null && fin.isBefore(debut)) {
            throw new InvalidRequestException("dateFinPrevue ne peut pas précéder datePrevue.");
        }

        r.setDescription(description);
        r.setNotes(notes);
        r.setDeclencheur(declencheur);
        r.setGestionnaire(gestionnaire);
        r.setCfgProspectId(ou(d.tierId(), r.getCfgProspectId()));
        r.setStartDatePrev(debut);
        r.setDelayPrev(ou(d.dureePrevue(), r.getDelayPrev()));
        r.setDueDatePrev(fin);
    }
}
