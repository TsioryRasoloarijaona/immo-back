package com.tsix.immocrm.domain;

/**
 * Disponibilité d'un article, calculée : {@code StkItem.ComFolderId} nul ou non, sans jointure
 * (un dossier sorti libère l'article). Réservé/vendu non distingués : frontière non confirmée.
 */
public enum StatutArticle {
    DISPONIBLE,
    OCCUPE
}
