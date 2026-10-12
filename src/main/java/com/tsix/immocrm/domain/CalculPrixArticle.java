package com.tsix.immocrm.domain;

import com.tsix.immocrm.entities.stk.StkItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Seul calcul des prix stockés d'un article (formules d'OMRAN, {@code Yes.Stk/Item.cs}) — voir
 * FEATURE-patrimoine-ecriture.md §5.5. Le TTC unitaire est arrondi avant d'être multiplié par la surface, comme OMRAN :
 * l'inverse donne 818 999,999975 au lieu de 819 000 sur l'article de contrôle ART344.
 */
public final class CalculPrixArticle {

    public static final int ECHELLE = 6;

    private CalculPrixArticle() {
    }

    /** Prix de l'article seul, à partir de ses saisies (surfaces, prix au m² HT, TVA en fraction). */
    public record Prix(BigDecimal m2ProvisoireTTC, BigDecimal m2DefinitifTTC,
                       BigDecimal articleProvisoireHT, BigDecimal articleProvisoireTTC,
                       BigDecimal articleDefinitifHT, BigDecimal articleDefinitifTTC) {
    }

    public static Prix prixArticle(BigDecimal surfaceProvisoire, BigDecimal surfaceDefinitive,
                                   BigDecimal m2ProvisoireHT, BigDecimal m2DefinitifHT, BigDecimal tva) {
        BigDecimal coefficient = BigDecimal.ONE.add(zero(tva));
        BigDecimal m2ProvisoireTTC = arrondi(zero(m2ProvisoireHT).multiply(coefficient));
        BigDecimal m2DefinitifTTC = arrondi(zero(m2DefinitifHT).multiply(coefficient));
        return new Prix(
                m2ProvisoireTTC,
                m2DefinitifTTC,
                arrondi(zero(m2ProvisoireHT).multiply(zero(surfaceProvisoire))),
                arrondi(m2ProvisoireTTC.multiply(zero(surfaceProvisoire))),
                arrondi(zero(m2DefinitifHT).multiply(zero(surfaceDefinitive))),
                arrondi(m2DefinitifTTC.multiply(zero(surfaceDefinitive))));
    }

    /** Pose les prix unitaires TTC et les prix article (HT/TTC, provisoire/définitif) depuis les saisies de l'article. */
    public static void appliquerPrixArticle(StkItem item) {
        Prix prix = prixArticle(item.getAreaFloor(), item.getArea(),
                item.getComSaleUnitCostFloorHT(), item.getComSaleUnitCostHT(), item.getVat());
        item.setComSaleUnitCostFloor(prix.m2ProvisoireTTC());
        item.setComSaleUnitCost(prix.m2DefinitifTTC());
        item.setSalePriceFloorHT(prix.articleProvisoireHT());
        item.setSalePriceFloor(prix.articleProvisoireTTC());
        item.setSalePriceHT(prix.articleDefinitifHT());
        item.setSalePrice(prix.articleDefinitifTTC());
    }

    /**
     * Prix de vente = prix de l'article + somme des prix article de ses sous-articles rattachés (aucun pour un
     * sous-article). Les prix article doivent déjà être posés ({@link #appliquerPrixArticle}).
     */
    public static void appliquerPrixDeVente(StkItem item, List<StkItem> sousArticles) {
        item.setComSalePriceFloorHT(somme(item, sousArticles, StkItem::getSalePriceFloorHT));
        item.setComSalePriceFloor(somme(item, sousArticles, StkItem::getSalePriceFloor));
        item.setComSalePriceHT(somme(item, sousArticles, StkItem::getSalePriceHT));
        item.setComSalePrice(somme(item, sousArticles, StkItem::getSalePrice));
    }

    private static BigDecimal somme(StkItem item, List<StkItem> sousArticles, Function<StkItem, BigDecimal> prix) {
        BigDecimal total = sousArticles.stream().map(prix).filter(Objects::nonNull)
                .reduce(zero(prix.apply(item)), BigDecimal::add);
        return arrondi(total);
    }

    private static BigDecimal arrondi(BigDecimal valeur) {
        return valeur.setScale(ECHELLE, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
