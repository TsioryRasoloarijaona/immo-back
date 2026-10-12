package com.tsix.immocrm.domain.chantier;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arithmétique commune du module Chantier : NULL vaut zéro, montants à 6 décimales comme les colonnes. */
public final class Montants {

    public static final int ECHELLE = 6;
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private Montants() {
    }

    public static BigDecimal ouZero(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }

    public static BigDecimal produit(BigDecimal a, BigDecimal b) {
        return ouZero(a).multiply(ouZero(b));
    }

    public static BigDecimal arrondi(BigDecimal valeur) {
        return ouZero(valeur).setScale(ECHELLE, RoundingMode.HALF_UP);
    }

    public static boolean estNul(BigDecimal valeur) {
        return valeur == null || valeur.signum() == 0;
    }

    /** Ratio OMRAN (0–1, {@code WarrantyRatio} 0,1) → pourcentage DTO (0–100, 10) ; {@code null} reste {@code null}. */
    public static BigDecimal enPourcentage(BigDecimal ratio) {
        return ratio == null ? null : ratio.movePointRight(2);
    }

    /** {@code numerateur / denominateur × 100} à 2 décimales ; {@code null} si le dénominateur est nul. */
    public static BigDecimal pourcentage(BigDecimal numerateur, BigDecimal denominateur) {
        if (estNul(denominateur)) {
            return null;
        }
        return ouZero(numerateur).multiply(CENT).divide(denominateur, 2, RoundingMode.HALF_UP);
    }
}
