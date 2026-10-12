package com.tsix.immocrm.services.equipe;

import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Même convention que {@code /api/kpi/commerciaux} : 30 jours par défaut, {@code au} inclus jusqu'à 23:59:59. */
record PeriodeEquipe(LocalDate du, LocalDate au, LocalDateTime debut, LocalDateTime fin) {

    private static final int JOURS_PAR_DEFAUT = 30;

    static PeriodeEquipe resoudre(LocalDate du, LocalDate au, LocalDate aujourdhui) {
        LocalDate fin = au != null ? au : aujourdhui;
        LocalDate debut = du != null ? du : fin.minusDays(JOURS_PAR_DEFAUT - 1L);
        if (debut.isAfter(fin)) {
            throw new InvalidRequestException(
                    "La date de début (" + debut + ") est postérieure à la date de fin (" + fin + ").");
        }
        return new PeriodeEquipe(debut, fin, debut.atStartOfDay(), fin.atTime(LocalTime.MAX));
    }
}
