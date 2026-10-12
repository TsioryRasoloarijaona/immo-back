package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/** Auteur et horodatage posés sur les lignes d'un onglet de tranche, une fois par enregistrement. */
@Component
class SignatureLigne {

    record Signature(String nom, LocalDateTime instant) {
    }

    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    SignatureLigne(UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    Signature courante() {
        return new Signature(utilisateurAffiche.courant().nom(), LocalDateTime.now(clock));
    }
}
