package com.tsix.immocrm.domain.projet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Montants d'un lot : marché / estimation (bordereau ou forfait), avancement, taux. Voir FEATURE-projet-chantier-lecture.md §5.2. */
public final class MontantsLot {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private MontantsLot() {
    }

    /** HT / TVA / TTC arrondis à 2 décimales ; {@code depuisBordereau} = faux si repli sur le forfait. */
    public record Trio(BigDecimal ht, BigDecimal tva, BigDecimal ttc, boolean depuisBordereau) {
    }

    /** Σ du bordereau si le lot a des lignes ({@code bordereau} non null), sinon le forfait saisi. */
    public static Trio choisir(Trio bordereau, BigDecimal forfaitHT, BigDecimal forfaitTVA, BigDecimal forfaitTTC) {
        if (bordereau != null) {
            return new Trio(arrondi(bordereau.ht()), arrondi(bordereau.tva()), arrondi(bordereau.ttc()), true);
        }
        return new Trio(arrondi(forfaitHT), arrondi(forfaitTVA), arrondi(forfaitTTC), false);
    }

    /** Cumul TTC / marché TTC × 100, 2 décimales, non plafonné ; null si le marché est nul. */
    public static BigDecimal avancementPct(BigDecimal cumulTTC, BigDecimal marcheTTC) {
        if (marcheTTC == null || marcheTTC.signum() == 0) {
            return null;
        }
        return z(cumulTTC).multiply(CENT).divide(marcheTTC, 2, RoundingMode.HALF_UP);
    }

    /** Montant × taux (fraction 0–1), 2 décimales. */
    public static BigDecimal applique(BigDecimal montant, BigDecimal ratio) {
        return arrondi(z(montant).multiply(z(ratio)));
    }

    /** Fraction 0–1 de la base → pourcentage 0–100 ; null reste null. */
    public static BigDecimal pourcentage(BigDecimal ratio) {
        return ratio == null ? null : ratio.multiply(CENT).stripTrailingZeros();
    }

    /** Pourcentage 0–100 saisi → fraction 0–1 de la base ; null reste null. */
    public static BigDecimal fraction(BigDecimal pourcentage) {
        return pourcentage == null ? null : pourcentage.movePointLeft(2);
    }

    /** PU TTC d'une ligne de bordereau = PU HT × (1 + FODEC) × (1 + TVA), ratios ; 6 décimales comme la colonne. */
    public static BigDecimal prixTTC(BigDecimal prixHT, BigDecimal fodecRatio, BigDecimal tvaRatio) {
        return z(prixHT).multiply(BigDecimal.ONE.add(z(fodecRatio))).multiply(BigDecimal.ONE.add(z(tvaRatio)))
                .setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Parts (ratios à 6 décimales) de chaque clé dans le total des montants, la dernière absorbant l'écart pour que la somme
     * vaille exactement 1 ; vide si le total n'est pas positif.
     */
    public static <K> Map<K, BigDecimal> parts(Map<K, BigDecimal> montants) {
        BigDecimal total = montants.values().stream().map(MontantsLot::z).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<K, BigDecimal> parts = new LinkedHashMap<>();
        if (total.signum() <= 0) {
            return parts;
        }
        BigDecimal reste = BigDecimal.ONE;
        List<K> cles = new ArrayList<>(montants.keySet());
        for (int i = 0; i < cles.size() - 1; i++) {
            BigDecimal part = z(montants.get(cles.get(i))).divide(total, 6, RoundingMode.HALF_UP);
            parts.put(cles.get(i), part);
            reste = reste.subtract(part);
        }
        parts.put(cles.get(cles.size() - 1), reste);
        return parts;
    }

    public static BigDecimal arrondi(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal z(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
