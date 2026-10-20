package com.tsix.immocrm.domain;

import java.util.List;

/**
 * Champ d'un référentiel administrable : {@code nom} sur le fil, {@code propriete} de l'entité (lue et écrite par
 * réflexion), règle O, longueur maximale d'un texte, table visée par une clé étrangère (FEATURE-patrimoine-referentiels.md §1).
 */
public record ChampReferentiel(String nom, String libelle, Type type, String propriete, boolean obligatoire,
                               Integer longueurMax, ReferenceAdmin reference) {

    public enum Type {
        TEXTE, ENTIER, DECIMAL, FRACTION, BOOLEEN, COULEUR, REFERENCE, OPERATEUR, LIGNES
    }

    /** Opérateurs de {@code CfgTrancheStatus.ThresholdOperator} ({@code ThresholdOperator.cs}). */
    public static final List<String> OPERATEURS = List.of("Sans contrôle", ">=", ">");

    public static final int COURT = 255;
    public static final int LONG = 500;

    public static final ChampReferentiel CODE = texte("code", "Code", "code", COURT).requis();
    public static final ChampReferentiel DESCRIPTION = texte("description", "Description", "description", COURT).requis();
    public static final ChampReferentiel SORTING = entier("sorting", "Ordre", "sorting");
    public static final ChampReferentiel NOTES = texte("notes", "Notes", "notes", LONG);

    public static ChampReferentiel texte(String nom, String libelle, String propriete, int longueurMax) {
        return new ChampReferentiel(nom, libelle, Type.TEXTE, propriete, false, longueurMax, null);
    }

    public static ChampReferentiel entier(String nom, String libelle, String propriete) {
        return de(nom, libelle, Type.ENTIER, propriete);
    }

    public static ChampReferentiel de(String nom, String libelle, Type type, String propriete) {
        return new ChampReferentiel(nom, libelle, type, propriete, false, null, null);
    }

    public static ChampReferentiel reference(String nom, String libelle, String propriete, ReferenceAdmin cible) {
        return new ChampReferentiel(nom, libelle, Type.REFERENCE, propriete, false, null, cible);
    }

    public ChampReferentiel requis() {
        return new ChampReferentiel(nom, libelle, type, propriete, true, longueurMax, reference);
    }
}
