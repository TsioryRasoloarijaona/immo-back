package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Montant HT d'une pénalité de retard ({@code ComDocument.cs:GeneratePenaltyDocument}) : forfait unique
 * {@code k × taux × HT de l'échéance}, ni par jour, ni plafonné, ni recalculé. Voir {@code FEATURE-impayes.md} P1.
 */
public final class PenaliteRetard {

    private static final int ECHELLE = 2;

    private PenaliteRetard() {
    }

    /**
     * {@code k} = reste / montant si un règlement existe et que le montant est positif, sinon 1.
     *
     * @param taux      {@code ComPenaltyRatio}, en fraction (0,05 = 5 %) ; {@code null} vaut zéro
     * @param montant   {@code Amount} de l'échéance (TTC) ; {@code null} vaut zéro
     * @param montantHT {@code AmountHT} de l'échéance ; {@code null} vaut zéro
     * @param regle     réglé de l'échéance avant le règlement en cours ; {@code null} vaut zéro
     */
    public static BigDecimal montantHT(BigDecimal taux, BigDecimal montant, BigDecimal montantHT, BigDecimal regle) {
        BigDecimal t = zeroSiNul(taux);
        BigDecimal m = zeroSiNul(montant);
        BigDecimal ht = zeroSiNul(montantHT);
        BigDecimal r = zeroSiNul(regle);
        BigDecimal base = t.multiply(ht);
        if (r.signum() > 0 && m.signum() > 0) {
            // Une seule division, en fin de calcul : k n'est jamais arrondi à part.
            return base.multiply(m.subtract(r)).divide(m, ECHELLE, RoundingMode.HALF_UP);
        }
        return base.setScale(ECHELLE, RoundingMode.HALF_UP);
    }

    private static BigDecimal zeroSiNul(BigDecimal valeur) {
        return valeur != null ? valeur : BigDecimal.ZERO;
    }
}
