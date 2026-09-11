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
        boolean parDefaut,

        @Schema(description = """
                Vraie si la colonne peut servir de clé à `?groupBy=` sur l'endpoint `/groupes` \
                de la liste. Le front pilote sa bande de groupement là-dessus, sans liste en dur.

                **Toutes les colonnes ne le sont pas, et c'est délibéré** : un montant porte \
                presque autant de valeurs distinctes que de lignes (190 prix pour 358 dossiers), \
                et les colonnes calculées par la vue légataire n'existent pas dans le graphe \
                d'entités. Voir `instructions/FEATURE-groupement-listes.md` §2.""",
                example = "true")
        boolean groupable,

        @Schema(description = "Vraie si la colonne peut être sommée via `?agregats=` — les bandes "
                + "« Total : » sous un en-tête de groupe. Concerne les colonnes MONTANT et NOMBRE.",
                example = "false")
        boolean agregeable
) {

    /** Construit depuis un catalogue : les deux booléens de groupement viennent du chemin déclaré. */
    public static ColonneDto de(String nom, String libelle, String groupe, String type,
                                boolean parDefaut, com.tsix.immocrm.domain.Chemin chemin) {
        return new ColonneDto(nom, libelle, groupe, type, parDefaut,
                chemin != null && chemin.groupable(),
                chemin != null && chemin.agregeable());
    }
}
