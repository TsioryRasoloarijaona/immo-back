package com.tsix.immocrm.domain;

/**
 * Granularité de groupement d'une date : jamais la valeur exacte, presque autant de groupes que de
 * lignes (FEATURE-groupement-listes.md §2).
 */
public enum Granularite {

    /** Clé {@code AAAA-MM-JJ}. */
    JOUR,

    /** Clé {@code AAAA-MM}. Défaut. */
    MOIS,

    /** Clé {@code AAAA}. */
    ANNEE;

    /** Valeur appliquée quand le client n'en passe aucune sur une colonne {@code DATE}. */
    public static final Granularite DEFAUT = MOIS;
}
