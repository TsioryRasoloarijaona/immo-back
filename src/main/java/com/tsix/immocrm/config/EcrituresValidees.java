package com.tsix.immocrm.config;

import java.util.List;

/**
 * Publié après validation d'une transaction d'écriture ; porte ses lignes car l'écouteur {@code @Async} tourne sur un
 * autre fil.
 *
 * @param lignes déjà ordonnées parent avant enfant
 */
public record EcrituresValidees(List<LigneEcrite> lignes) {
}
