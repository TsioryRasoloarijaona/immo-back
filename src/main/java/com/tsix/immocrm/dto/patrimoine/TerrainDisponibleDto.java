package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Terrain proposé à l'onglet Terrains d'une tranche (StkItem nature TER, vocation PROJET).")
public record TerrainDisponibleDto(
        @Schema(description = "StkItem.id.")
        UUID id,
        @Schema(description = "Code du terrain.")
        String code,
        @Schema(description = "Libellé du terrain.")
        String description,
        @Schema(description = "Superficie (StkItem.Area, m²).")
        BigDecimal surface
) {
}
