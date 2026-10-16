package com.tsix.immocrm.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Résumés d'une liste plate calculés sur toutes les lignes du filtre, pas sur la page.")
public record TotauxListeDto(

        @Schema(description = "Nombre de lignes du filtre : égale le `totalElements` de la liste.", example = "637")
        long nombreTotal,

        @Schema(example = "{\"surface:AVG\": 112.4, \"code:COUNT\": 637, \"dateDocument:MIN\": \"2026-09-16T08:30:00\"}")
        Map<String, Object> totaux) {
}
