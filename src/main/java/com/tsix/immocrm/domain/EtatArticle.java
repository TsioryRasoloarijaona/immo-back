package com.tsix.immocrm.domain;

/** Filtre « État » d'OMRAN : dossier porté par l'article lui-même, pas par son parent (FEATURE-patrimoine-sous-articles.md R4-6). */
public enum EtatArticle {
    /** Ni dossier de vente ni dossier de location. */
    LIBRE,
    /** {@code ComFolderId} renseigné. */
    VENDU,
    /** {@code RntFolderId} renseigné. */
    LOUE
}
