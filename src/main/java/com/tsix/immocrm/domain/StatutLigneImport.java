package com.tsix.immocrm.domain;

/**
 * Sort d'une ligne d'un import CSV de leads — {@code POST /api/pre-prospections/import} et son
 * aperçu. Voir {@code instructions/FEATURE-creation-pre-prospection.md} §5.
 */
public enum StatutLigneImport {

    /** Aucun contact identique connu : créée (ou le sera, en aperçu). */
    NOUVELLE,

    /**
     * Même contact qu'une prospection existante, mais une autre soumission (date différente) :
     * créée quand même et signalée — OMRAN enregistre une ligne par soumission.
     */
    DOUBLON,

    /**
     * Même contact <b>et</b> même date de soumission qu'une ligne existante, en base ou plus haut
     * dans le fichier : c'est le même lead, réimporté. Ignorée — réimporter un fichier ne crée
     * rien deux fois.
     */
    DEJA_IMPORTEE,

    /** Ligne illisible (nom vide, date absente ou dans un autre format…) : ignorée. */
    ERREUR
}
