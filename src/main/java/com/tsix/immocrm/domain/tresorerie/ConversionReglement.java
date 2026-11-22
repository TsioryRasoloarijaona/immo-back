package com.tsix.immocrm.domain.tresorerie;

import com.tsix.immocrm.domain.achat.CalculReglement;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;

/**
 * Seuls calculs d'un règlement de trésorerie (FEATURE-n3-recettes-depenses.md D6, R6) : multiplicateur de conversion
 * ({@code Yes.Stl.Commun/Settlement.cs:ConvertMultiplier}), reste à affecter et retard.
 */
public final class ConversionReglement {

    private static final String POINTE = "POINTED";

    private ConversionReglement() {
    }

    /** {@code ConvertUnit} nul ou 0 → 1, sinon {@code ConvertValue / ConvertUnit} ; valeur absente → 1. */
    public static BigDecimal multiplicateur(BigDecimal valeur, Integer unite) {
        if (unite == null || unite == 0 || valeur == null) {
            return BigDecimal.ONE;
        }
        return valeur.divide(BigDecimal.valueOf(unite), MathContext.DECIMAL64);
    }

    /** {@code AmountBase × multiplicateur − Σ lignes} ; montants absents = 0. */
    public static BigDecimal resteAAffecter(BigDecimal base, BigDecimal valeur, Integer unite, BigDecimal affecte) {
        return CalculReglement.resteAAffecter(nonNul(base).multiply(multiplicateur(valeur, unite)), affecte);
    }

    /** Échéance ≤ {@code aujourdhui} et statut ≠ POINTED (NULL compris). */
    public static boolean enRetard(LocalDate echeance, String statut, LocalDate aujourdhui) {
        return echeance != null && !echeance.isAfter(aujourdhui) && !POINTE.equals(statut);
    }

    private static BigDecimal nonNul(BigDecimal montant) {
        return montant == null ? BigDecimal.ZERO : montant;
    }
}
