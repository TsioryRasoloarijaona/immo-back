package com.tsix.immocrm.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Catalogue des colonnes de {@code GET /api/suivi-commerciaux}, sous les noms de {@code LigneSuiviCommerciauxDto}
 * (FEATURE-suivi-commerciaux.md §3). Personnes distinctes : non agrégeable ici, calculée par la synthèse (§7.4).
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ColonneSuiviCommerciaux implements Colonne {

    ID("id", "Identifiant", TypeColonne.TEXTE, null),
    JOUR("jour", "Jour", TypeColonne.DATE, Chemin.groupable("jour")),
    DATE_SAISIE("dateSaisie", "Date", TypeColonne.DATE, Chemin.date("dateSaisie")),
    COMMERCIAL_NOM("commercialNom", "Commercial", TypeColonne.TEXTE, Chemin.groupable("commercialNom")),
    TYPE("type", "Type", TypeColonne.TEXTE, Chemin.groupable("type")),
    RESULTAT("resultat", "Résultat", TypeColonne.TEXTE, Chemin.groupable("resultat")),
    JOINT("joint", "Joint", TypeColonne.BOOLEEN, Chemin.groupable("joint")),
    PERSONNE_NOM("personneNom", "Personne", TypeColonne.TEXTE, Chemin.groupable("personneNom")),
    PROSPECTION_ID("prospectionId", "Id prospection", TypeColonne.TEXTE, null),
    PROJET("projet", "Projet", TypeColonne.TEXTE, Chemin.groupable("projet"));

    public enum TypeColonne { TEXTE, DATE, BOOLEEN }

    /** Identifiant du filtre Jackson de {@code LigneSuiviCommerciauxDto}. */
    public static final String FILTRE = "colonnesSuiviCommerciaux";

    private final String nom;
    private final String libelle;
    private final TypeColonne type;
    private final Chemin chemin;

    @Override
    public TypeExport typeExport() {
        return TypeExport.valueOf(type.name());
    }

    /** Toutes les colonnes sont servies sans {@code ?colonnes=}. */
    @Override
    public boolean parDefaut() {
        return true;
    }
}
