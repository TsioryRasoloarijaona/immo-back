package com.tsix.immocrm.config;

import java.util.List;

/**
 * Publié après validation d'une transaction d'écriture ; porte ses lignes car la collecte, liée au fil, est vidée en
 * fin de transaction.
 *
 * @param lignes déjà ordonnées parent avant enfant
 */
public record EcrituresValidees(List<LigneEcrite> lignes) {
}
