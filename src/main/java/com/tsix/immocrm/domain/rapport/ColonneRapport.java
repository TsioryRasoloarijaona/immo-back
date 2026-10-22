package com.tsix.immocrm.domain.rapport;

import com.tsix.immocrm.domain.ColonneLibre;
import com.tsix.immocrm.domain.TypeExport;

/**
 * Colonne d'une section de rapport. {@code groupe} = en-tête de groupe de colonnes (facultatif) ; {@code decimales}
 * (B6) = décimales affichées d'un {@code NOMBRE}, 0 si absent.
 */
public record ColonneRapport(String nom, String libelle, TypeColonne type, String groupe, FonctionTotal fonctionTotal,
                             boolean parDefaut, Integer decimales) implements ColonneLibre {

    public ColonneRapport {
        fonctionTotal = fonctionTotal == null ? FonctionTotal.AUCUNE : fonctionTotal;
        decimales = decimales == null ? 0 : decimales;
    }

    public static ColonneRapport texte(String nom, String libelle) {
        return new ColonneRapport(nom, libelle, TypeColonne.TEXTE, null, FonctionTotal.AUCUNE, true, 0);
    }

    /** Nombre entier sommé dans le total. */
    public static ColonneRapport effectif(String nom, String libelle, String groupe) {
        return new ColonneRapport(nom, libelle, TypeColonne.NOMBRE, groupe, FonctionTotal.SOMME, true, 0);
    }

    public static ColonneRapport pourcentage(String nom, String libelle, String groupe) {
        return new ColonneRapport(nom, libelle, TypeColonne.POURCENTAGE, groupe, FonctionTotal.AUCUNE, true, 0);
    }

    public static ColonneRapport montant(String nom, String libelle, String groupe) {
        return new ColonneRapport(nom, libelle, TypeColonne.MONTANT, groupe, FonctionTotal.SOMME, true, 0);
    }

    @Override
    public TypeExport typeExport() {
        return switch (type) {
            case MONTANT -> TypeExport.MONTANT;
            case NOMBRE, POURCENTAGE -> TypeExport.NOMBRE;
            case DATE -> TypeExport.DATE;
            case BOOLEEN -> TypeExport.BOOLEEN;
            case TEXTE -> TypeExport.TEXTE;
        };
    }
}
