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

        @Schema(description = "Regroupement pour présenter le sélecteur par sections ; valeurs propres à chaque catalogue.",
                example = "ACTIVITE")
        String groupe,

        @Schema(description = "Type d'affichage — permet d'aligner et de formater sans table de "
                + "correspondance côté front. TAUX = fraction (0.2 = 20 %).",
                example = "NOMBRE",
                allowableValues = {"TEXTE", "NOMBRE", "MONTANT", "TAUX", "DATE", "BOOLEEN"})
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
}
