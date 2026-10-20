package com.tsix.immocrm.dto.referentiel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ColonneDto(

        @Schema(description = "Nom technique, à passer tel quel dans `?colonnes=`.",
                example = "joursInactivite")
        String nom,

        @Schema(description = "Libellé d'en-tête, en français, prêt à afficher.",
                example = "Jours d'inactivité")
        String libelle,

        @Schema(description = "Regroupement pour présenter le sélecteur par sections.",
                example = "ACTIVITE",
                allowableValues = {"IDENTITE", "CONTACT", "CONTEXTE", "FINANCIER", "ACTIVITE", "CONVERSION"})
        String groupe,

        @Schema(description = "Type d'affichage — permet d'aligner et de formater sans table de "
                + "correspondance côté front.",
                example = "NOMBRE",
                allowableValues = {"TEXTE", "NOMBRE", "MONTANT", "POURCENTAGE", "DATE", "BOOLEEN"})
        String type,

        @Schema(example = "false")
        boolean parDefaut,

        @Schema(example = "true")
        boolean groupable,

        @Schema(example = "false")
        boolean agregeable,

        @Schema(example = "[\"SUM\", \"AVG\", \"MIN\", \"MAX\", \"COUNT\"]")
        List<String> fonctions
) {

    /** Groupement et fonctions se déduisent du chemin, jamais saisis à la main dans un catalogue. */
    public static ColonneDto de(String nom, String libelle, String groupe, String type,
                                boolean parDefaut, com.tsix.immocrm.domain.Chemin chemin) {
        return new ColonneDto(nom, libelle, groupe, type, parDefaut,
                chemin != null && chemin.groupable(),
                chemin != null && chemin.agregeable(),
                com.tsix.immocrm.domain.FonctionAgregat.pour(chemin).stream().map(Enum::name).toList());
    }

    /** Colonne sans chemin dont le serveur calcule la seule somme (montant calculé en Java). */
    public static ColonneDto sommeCalculee(String nom, String libelle, String groupe, String type, boolean parDefaut) {
        return new ColonneDto(nom, libelle, groupe, type, parDefaut, false, false,
                List.of(com.tsix.immocrm.domain.FonctionAgregat.SUM.name()));
    }
}
