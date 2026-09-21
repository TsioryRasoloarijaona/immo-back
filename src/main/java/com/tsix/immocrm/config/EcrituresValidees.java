package com.tsix.immocrm.config;

import java.util.List;

/**
 * Événement publié quand une transaction d'écriture est sur le point d'être validée.
 *
 * <p>Il transporte sa charge utile plutôt que de la laisser dans {@link LignesEcrites} :
 * l'écouteur qui le consomme est {@code @Async}, il s'exécute donc sur un <b>autre fil</b>, où
 * la variable liée au fil de la requête n'existe plus.
 *
 * @param lignes les lignes écrites, déjà ordonnées parent avant enfant
 */
public record EcrituresValidees(List<LigneEcrite> lignes) {
}
