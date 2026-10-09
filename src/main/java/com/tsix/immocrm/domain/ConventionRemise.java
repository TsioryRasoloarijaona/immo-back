package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Échelle de la colonne {@code Discount} (ComFolder, StlQuote, StlQuoteItem) : OMRAN y range un ratio
 * 0–1 du HT avant remise, l'API reçoit et expose un pourcentage 0–100. Voir FEATURE-echelle-remises.md.
 */
public final class ConventionRemise {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    /** Échelle des colonnes {@code decimal(26,6)} : le ratio relu en base est celui-ci, pas plus fin. */
    private static final int ECHELLE_COLONNE = 6;

    private ConventionRemise() {
    }

    /** Pourcentage saisi (5 = 5 %) → ratio rangé en base (0.05). {@code null} reste {@code null}. */
    public static BigDecimal ratioDepuisPourcentage(BigDecimal pourcentage) {
        return pourcentage == null ? null : pourcentage.divide(CENT, ECHELLE_COLONNE, RoundingMode.HALF_UP);
    }

    /** Ratio OMRAN d'un dossier : remise HT / prix HT avant remise ; 0 sans base HT. */
    public static BigDecimal ratioDuHT(BigDecimal remiseHT, BigDecimal prixAvantRemiseHT) {
        if (remiseHT == null || prixAvantRemiseHT == null || prixAvantRemiseHT.signum() == 0) {
            return BigDecimal.ZERO.setScale(ECHELLE_COLONNE);
        }
        return remiseHT.divide(prixAvantRemiseHT, ECHELLE_COLONNE, RoundingMode.HALF_UP);
    }

    /** Ratio lu en base → pourcentage exposé par l'API (0.05 → 5). {@code null} reste {@code null}. */
    public static BigDecimal pourcentage(BigDecimal ratio) {
        return ratio == null ? null : ratio.multiply(CENT);
    }
}
