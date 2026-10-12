package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Seul calcul HT ↔ TTC d'un frais de vente de tranche ({@code CfgTrancheSettlementCategory} d'OMRAN) :
 * TTC = HT × (1 + TVA), HT = TTC / (1 + TVA), TVA en fraction. Échelle et arrondi de {@link CalculPrixArticle}.
 */
public final class CalculFraisVente {

    public record Montants(BigDecimal ht, BigDecimal ttc) {
    }

    private CalculFraisVente() {
    }

    /** {@code saisiEnTtc} : le montant saisi est le TTC, sinon le HT ; l'autre est calculé. */
    public static Montants depuisSaisie(BigDecimal montant, BigDecimal tva, boolean saisiEnTtc) {
        BigDecimal coefficient = BigDecimal.ONE.add(zero(tva));
        BigDecimal saisi = arrondi(zero(montant));
        if (saisiEnTtc) {
            return new Montants(saisi.divide(coefficient, CalculPrixArticle.ECHELLE, RoundingMode.HALF_UP), saisi);
        }
        return new Montants(saisi, arrondi(saisi.multiply(coefficient)));
    }

    private static BigDecimal arrondi(BigDecimal valeur) {
        return valeur.setScale(CalculPrixArticle.ECHELLE, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
