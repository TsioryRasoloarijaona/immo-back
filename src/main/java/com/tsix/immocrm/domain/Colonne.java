package com.tsix.immocrm.domain;

/**
 * Contrat commun aux catalogues de colonnes, pour que {@link Colonnes} résolve {@code ?colonnes=}
 * une seule fois. Groupe et type restent typés par catalogue (pas dégradés en {@code String}).
 */
public sealed interface Colonne
        permits ColonneDossier, ColonneEncaissement, ColonneProspect, ColonneProspection,
                ColonneSuiviAction {

    /** Nom technique, tel qu'il est passé dans {@code ?colonnes=} et sérialisé en JSON. */
    String nom();

    /** Vrai si la colonne est servie quand {@code ?colonnes=} est absent. */
    boolean parDefaut();

    /**
     * {@code null} = ni groupable ni agrégeable, et c'est voulu : une colonne n'entre au groupement
     * que déclarée explicitement, chemin vérifié contre l'entité (FEATURE-groupement-listes.md §2).
     */
    default Chemin chemin() {
        return null;
    }
}
