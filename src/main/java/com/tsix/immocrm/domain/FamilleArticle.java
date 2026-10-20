package com.tsix.immocrm.domain;

/**
 * Famille d'un article de l'état des ventes, tirée du code de catégorie (FEATURE-tdb-vente-vendus-libres.md R3) ;
 * le nom de la constante est la valeur de la colonne {@code Famille} du {@code @Subselect} de {@code StkItemEtatVentes}.
 */
public enum FamilleArticle {
    APPARTEMENT("Appartements"),
    PARKING("Parkings"),
    MAGASIN("Magasins"),
    AUTRE("Autres");

    private final String libelle;

    FamilleArticle(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
