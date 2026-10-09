package com.tsix.immocrm.domain;

import com.tsix.immocrm.entities.stk.StkItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Prix de référence d'un article, seule base des montants (dossier, simulation, devis, DTO article).
 * {@code ComSalePrice} n'est jamais NULL en base : un prix manquant vaut 0. Voir FEATURE-lot4 §1.
 */
public final class PrixArticle {

    private PrixArticle() {
    }

    /** Prix de référence TTC, son HT (renseigné ou déduit de la TVA) et s'il vient du prix plancher. */
    public record PrixDeReference(BigDecimal ttc, BigDecimal htRenseigne, boolean estPlancher) {

        public BigDecimal ht(BigDecimal vatRatio) {
            return htRenseigne != null ? htRenseigne : diviserParTaux(ttc, vatRatio);
        }
    }

    /** {@code ComSalePrice} > 0, sinon {@code ComSalePriceFloor} > 0, sinon vide. */
    public static Optional<PrixDeReference> prixDeReference(StkItem article) {
        if (estPositif(article.getComSalePrice())) {
            return Optional.of(new PrixDeReference(article.getComSalePrice(), article.getComSalePriceHT(), false));
        }
        if (estPositif(article.getComSalePriceFloor())) {
            return Optional.of(new PrixDeReference(article.getComSalePriceFloor(), article.getComSalePriceFloorHT(), true));
        }
        return Optional.empty();
    }

    /** Prix de référence TTC, ou 0 s'il n'y en a pas (sous-articles, devis sans prix imposé). */
    public static BigDecimal ttcOuZero(StkItem article) {
        return prixDeReference(article).map(PrixDeReference::ttc).orElse(BigDecimal.ZERO);
    }

    /** Prix de référence HT, ou 0 s'il n'y en a pas. */
    public static BigDecimal htOuZero(StkItem article, BigDecimal vatRatio) {
        return prixDeReference(article).map(prix -> prix.ht(vatRatio)).orElse(BigDecimal.ZERO);
    }

    /** HT = TTC / (1 + taux), TTC tel quel si le taux est nul ou absent. */
    public static BigDecimal diviserParTaux(BigDecimal montantTTC, BigDecimal vatRatio) {
        if (vatRatio == null || vatRatio.signum() == 0) {
            return montantTTC;
        }
        return montantTTC.divide(BigDecimal.ONE.add(vatRatio), 6, RoundingMode.HALF_UP);
    }

    private static boolean estPositif(BigDecimal montant) {
        return montant != null && montant.signum() > 0;
    }
}
