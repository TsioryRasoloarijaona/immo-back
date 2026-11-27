package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.services.perimetre.AgencesDesVendeurs;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Horloge et agence du vendeur, regroupées pour le constructeur de {@link DossierService} (S107). */
@Component
public record LecturesDossier(Clock clock, AgencesDesVendeurs agences) {
}
