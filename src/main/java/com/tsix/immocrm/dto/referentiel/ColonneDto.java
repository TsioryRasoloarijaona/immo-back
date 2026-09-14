package com.tsix.immocrm.dto.referentiel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

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
        boolean agregeable,

        @Schema(description = """
                Fonctions d'agrégat que **cette colonne** accepte dans `?agregats=`, dans l'ordre \
                où le menu Σ doit les proposer. C'est la liste à piloter pour construire ce menu — \
                aucune ne doit être codée en dur côté front.

                Elle n'est pas la même pour toutes : un montant accepte les cinq, une **date** \
                n'accepte que `MIN`/`MAX`/`COUNT` (sommer une date n'a pas de sens), une colonne \
                calculée par différence n'accepte ni `MIN` ni `MAX`, et une colonne sans chemin \
                rend une liste **vide** — elle n'existe pas en base, il n'y a rien à agréger.

                Rappel : un nom nu dans `?agregats=` vaut toujours `SUM`. `agregeable` ci-dessus \
                reste donc vrai exactement quand `SUM` figure ici.""",
                example = "[\"SUM\", \"AVG\", \"MIN\", \"MAX\", \"COUNT\"]")
        List<String> fonctions
) {

    /**
     * Construit depuis un catalogue : les booléens de groupement <em>et</em> la liste des fonctions
     * viennent du chemin déclaré. Aucune des trois n'est saisie à la main dans les catalogues, ce
     * qui leur évite de mentir sur ce que le moteur sait faire.
     */
    public static ColonneDto de(String nom, String libelle, String groupe, String type,
                                boolean parDefaut, com.tsix.immocrm.domain.Chemin chemin) {
        return new ColonneDto(nom, libelle, groupe, type, parDefaut,
                chemin != null && chemin.groupable(),
                chemin != null && chemin.agregeable(),
                com.tsix.immocrm.domain.FonctionAgregat.pour(chemin).stream().map(Enum::name).toList());
    }
}
