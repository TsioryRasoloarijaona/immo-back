package com.tsix.immocrm.services.tresorerie;

import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.dossier.ComptePointage;
import com.tsix.immocrm.services.dossier.ReferencesOmran;
import com.tsix.immocrm.services.dossier.ReglementOmran;
import com.tsix.immocrm.services.droits.DroitsOmran;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Dépendances partagées de la saisie d'un décaissement (regroupées pour rester sous S107). */
@Component
public record OutilsSaisieDecaissement(DroitsOmran droits, ComptePointage pointage, ReglementOmran reglementOmran,
                                       ReferencesOmran references, UtilisateurAffiche utilisateurs, Clock clock) {
}
