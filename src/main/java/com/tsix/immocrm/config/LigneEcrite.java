package com.tsix.immocrm.config;

import java.util.UUID;

/**
 * Table et clé seulement, pas d'entité : le rapatriement relit la ligne telle que la base l'a enregistrée.
 *
 * @param table nom physique de {@code @Table(name = ...)}
 */
public record LigneEcrite(String table, UUID pkey) {
}
