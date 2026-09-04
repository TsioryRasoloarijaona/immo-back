package com.tsix.immocrm.storage;

/**
 * Résultat d'un dépôt sur le stockage objet — ce qu'il faut enregistrer en base pour
 * retrouver et contrôler le fichier ensuite ({@code AppFileObject}).
 *
 * @param cle          clé complète dans le bucket, telle qu'elle sera stockée en base et
 *                     <b>jamais recalculée</b> à la relecture
 * @param tailleOctets taille réellement déposée
 * @param contentType  type MIME servi au téléchargement
 * @param sha256       empreinte hexadécimale minuscule, transmise au serveur qui l'a
 *                     revérifiée à l'écriture
 */
public record ObjetStocke(String cle, long tailleOctets, String contentType, String sha256) {
}
