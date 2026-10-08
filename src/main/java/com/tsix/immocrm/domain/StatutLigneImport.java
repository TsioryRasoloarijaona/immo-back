package com.tsix.immocrm.domain;

/**
 * Sort d'une ligne d'un import CSV de leads — {@code POST /api/pre-prospections/import} et son
 * aperçu. Voir {@code instructions/FEATURE-creation-pre-prospection.md} §5.
 */
public enum StatutLigneImport {

    /** Aucun contact identique connu : créée si sélectionnée (ou le sera, en aperçu). */
    NOUVELLE,

    /** Même contact, autre date de soumission : créée si sélectionnée et signalée — OMRAN garde une ligne par soumission. */
    DOUBLON,

    /** Même contact et même date, en base ou plus haut dans le fichier : ignorée (réimport idempotent). */
    DEJA_IMPORTEE,

    /** Ligne illisible (nom vide, date absente ou dans un autre format…) : ignorée. */
    ERREUR
}
