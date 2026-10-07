package com.tsix.immocrm.dto.dossier;

/** Un statut atteignable, et la colonne jalon où {@code PATCH .../statut} écrira sa {@code date} (s'il y en a une). */
public record EtapeSuivanteDto(
        String statut,
        String champDate,
        String libelleChampDate,
        String confiance
) {
}
