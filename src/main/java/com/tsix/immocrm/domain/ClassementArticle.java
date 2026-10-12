package com.tsix.immocrm.domain;

import java.util.Objects;

/**
 * Règle unique du classement d'un article bâti (FEATURE-patrimoine-ecriture.md §5.2, §6) : catégorie et type de nature
 * « Bâtiment » et paramétrés ou non « Pour les sous articles » ; vocation « Projet » réservée aux terrains. Sert aux
 * listes déroulantes et au contrôle des écritures.
 */
public final class ClassementArticle {

    public static final String NATURE_BATIMENT = "BAT";
    public static final String NATURE_TERRAIN = "TER";
    public static final String VOCATION_PROJET = "PROJET";
    public static final String VOCATION_VENTE = "VENTE";

    private ClassementArticle() {
    }

    /** {@code forUnderItem} NULL ne vaut ni 0 ni 1, comme le filtre {@code forUnderItem = 0} d'OMRAN. */
    public static boolean classementEligible(String codeNature, Boolean forUnderItem, boolean pourSousArticle) {
        return NATURE_BATIMENT.equalsIgnoreCase(codeNature) && Objects.equals(forUnderItem, pourSousArticle);
    }

    /** Catégorie d'article ou de sous-article bâti : celles du plancher couvert d'une tranche. */
    public static boolean categorieBatie(String codeNature, Boolean forUnderItem) {
        return classementEligible(codeNature, forUnderItem, false) || classementEligible(codeNature, forUnderItem, true);
    }

    public static boolean vocationEligible(String codeVocation) {
        return !VOCATION_PROJET.equalsIgnoreCase(codeVocation);
    }
}
