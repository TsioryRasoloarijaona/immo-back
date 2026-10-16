package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Règle de l'impayé de vente (FEATURE-dossier-durcissement.md §1, {@code StlImpayedView}) : catégorie au crédit,
 * statut de vente effective, reste &gt; 0 et échéance au plus tard aujourd'hui. Ici la borne de date et le reste ;
 * la catégorie et le statut sont filtrés en SQL ({@code SpecImpayes}).
 */
public final class Impaye {

    /** {@code AdmParamsInteger.CurrencyDecimalDigits} vaut 2 en production. */
    private static final int ECHELLE_MONTANT = 2;

    private Impaye() {
    }

    /** « ≤ aujourd'hui » s'écrit {@code DocDate < demain 00:00} : une échéance du jour est échue. */
    public static LocalDateTime borneEchue(LocalDate aujourdhui) {
        return aujourdhui.plusDays(1).atStartOfDay();
    }

    public static boolean echue(LocalDateTime dateEcheance, LocalDate aujourdhui) {
        return dateEcheance != null && dateEcheance.isBefore(borneEchue(aujourdhui));
    }

    /** {@code Amount − réglé} (réglé signé, règlements annulés exclus), arrondi au centime ; null vaut zéro. */
    public static BigDecimal reste(BigDecimal montant, BigDecimal regle) {
        return (montant != null ? montant : BigDecimal.ZERO)
                .subtract(regle != null ? regle : BigDecimal.ZERO)
                .setScale(ECHELLE_MONTANT, RoundingMode.HALF_UP);
    }

    public static boolean resteDu(BigDecimal montant, BigDecimal regle) {
        return reste(montant, regle).signum() > 0;
    }
}
