package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * Prix d'un lot comme OMRAN ({@code Yes.Stk/Item.cs}) : intermédiaires en DECIMAL128, arrondi à l'échelle 6 HALF_UP
 * seulement au stockage — voir FEATURE-patrimoine-ecriture.md §5.5 et §14.1.
 */
public final class CalculPrixArticle {

    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final int ECHELLE = 6;

    private CalculPrixArticle() {
    }

    public record PrixLot(BigDecimal prixM2HT, BigDecimal prixM2TTC, BigDecimal prixHT, BigDecimal prixTTC) {
    }

    public static PrixLot depuisHT(BigDecimal surface, BigDecimal prixM2HT, BigDecimal tauxTva) {
        BigDecimal prixM2TTC = prixM2HT.multiply(BigDecimal.ONE.add(tauxTva), PRECISION);
        return arrondi(prixM2HT, prixM2TTC, prixM2HT.multiply(surface, PRECISION), prixM2TTC.multiply(surface, PRECISION));
    }

    public static PrixLot depuisTTC(BigDecimal surface, BigDecimal prixM2TTC, BigDecimal tauxTva) {
        BigDecimal prixM2HT = prixM2TTC.divide(BigDecimal.ONE.add(tauxTva), PRECISION);
        return arrondi(prixM2HT, prixM2TTC, prixM2HT.multiply(surface, PRECISION), prixM2TTC.multiply(surface, PRECISION));
    }

    /** Prix de vente d'un article = le sien + ceux de ses sous-articles rattachés ; prix au m² inchangés. */
    public static PrixLot avecSousArticles(PrixLot article, List<PrixLot> sousArticles) {
        BigDecimal prixHT = sousArticles.stream().map(PrixLot::prixHT).reduce(article.prixHT(), BigDecimal::add);
        BigDecimal prixTTC = sousArticles.stream().map(PrixLot::prixTTC).reduce(article.prixTTC(), BigDecimal::add);
        return arrondi(article.prixM2HT(), article.prixM2TTC(), prixHT, prixTTC);
    }

    private static PrixLot arrondi(BigDecimal prixM2HT, BigDecimal prixM2TTC, BigDecimal prixHT, BigDecimal prixTTC) {
        return new PrixLot(echelle(prixM2HT), echelle(prixM2TTC), echelle(prixHT), echelle(prixTTC));
    }

    private static BigDecimal echelle(BigDecimal valeur) {
        return valeur.setScale(ECHELLE, RoundingMode.HALF_UP);
    }
}
