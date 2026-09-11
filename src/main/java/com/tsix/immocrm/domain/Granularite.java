package com.tsix.immocrm.domain;

/**
 * Granularité de groupement d'une colonne {@code DATE}.
 *
 * <h2>Pourquoi une date ne se groupe jamais telle quelle</h2>
 *
 * <p>Mesuré sur {@code yesRemsProd} : les 358 dossiers portent <strong>214</strong>
 * {@code DocDate} distinctes, les 17 939 prospections en portent <strong>16 588</strong>. Grouper
 * sur la valeur exacte rendrait donc presque autant de groupes que de lignes — une liste plate
 * déguisée, plus lente que la liste elle-même puisqu'elle impose une agrégation.
 *
 * <p>Ramenées au mois, les mêmes colonnes donnent 96 et 102 groupes : de l'information. C'est
 * pourquoi {@link #MOIS} est le défaut et qu'aucune granularité « valeur exacte » n'est offerte —
 * {@link #JOUR} est déjà le grain le plus fin utile.
 *
 * <p>Sans JPA, comme le reste de {@code domain/}.
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
