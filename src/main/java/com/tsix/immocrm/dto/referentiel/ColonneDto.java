package com.tsix.immocrm.dto.referentiel;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Une colonne sélectionnable d'une liste, telle que la publie le backend au sélecteur de colonnes.
 */
@Schema(description = "Description d'une colonne sélectionnable. Le front construit son sélecteur "
        + "à partir de cette liste plutôt que d'en coder une en dur — quand une colonne est "
        + "ajoutée ou renommée côté serveur, le sélecteur suit sans redéploiement.")
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
                allowableValues = {"TEXTE", "NOMBRE", "MONTANT", "DATE", "BOOLEEN"})
        String type,

        @Schema(description = "Vraie pour les colonnes renvoyées quand `?colonnes=` est absent. "
                + "L'ensemble des colonnes `parDefaut` reproduit exactement la réponse "
                + "historique de la liste.",
                example = "false")
        boolean parDefaut
) {
}
