package com.tsix.immocrm.storage;

/**
 * Ce qu'il faut enregistrer dans {@code AppFileObject} après un dépôt.
 *
 * @param cle    clé complète, stockée telle quelle et jamais recalculée à la relecture
 * @param sha256 hexadécimal minuscule, revérifié par le serveur à l'écriture
 */
public record ObjetStocke(String cle, long tailleOctets, String contentType, String sha256) {
}
