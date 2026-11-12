package com.tsix.immocrm.domain.projet;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** R8 de FEATURE-lot-fiche-parite.md : chaque date renseignée ≥ toutes les dates renseignées qui la précèdent. */
public final class VerificationDatesLot {

    private VerificationDatesLot() {
    }

    public record DateNommee(String libelle, LocalDate date) {
    }

    /** Message du premier couple fautif (dans l'ordre de la chaîne), vide si la chaîne est croissante. */
    public static Optional<String> premiereErreur(List<DateNommee> chaine) {
        DateNommee plusTardive = null;
        for (DateNommee d : chaine) {
            if (d.date() == null) {
                continue;
            }
            if (plusTardive == null || d.date().isAfter(plusTardive.date())) {
                plusTardive = d;
            } else if (d.date().isBefore(plusTardive.date())) {
                return Optional.of("La date '%s' doit être supérieure ou égale à la date '%s'."
                        .formatted(d.libelle(), plusTardive.libelle()));
            }
        }
        return Optional.empty();
    }
}
