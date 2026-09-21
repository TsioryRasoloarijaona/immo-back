package com.tsix.immocrm.config;

import java.util.UUID;

/**
 * Une ligne que la transaction courante vient d'écrire sur la source de vérité.
 *
 * <p>Volontairement réduite à une désignation : nom de table et clé. <b>Aucune entité n'est
 * retenue</b> — elle serait détachée au moment du rapatriement, et le rapatriement relit de
 * toute façon la ligne telle que la base l'a réellement enregistrée, déclencheurs et valeurs
 * par défaut compris.
 *
 * @param table nom physique, tel qu'écrit dans {@code @Table(name = ...)}
 * @param pkey  valeur de la colonne {@code PKey}
 */
public record LigneEcrite(String table, UUID pkey) {
}
