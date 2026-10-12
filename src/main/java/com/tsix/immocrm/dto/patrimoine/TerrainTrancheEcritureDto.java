package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne voulue de l'onglet Terrains ; son rang dans la liste devient son ordre.")
public record TerrainTrancheEcritureDto(
        @Schema(description = "Id d'une ligne existante de cette tranche ; absent = nouvelle ligne.")
        UUID id,
        @Schema(description = "Terrain, voir GET /api/tranches/terrains-disponibles. Obligatoire.")
        UUID terrainId,
        @Schema(description = "Quote-part en fraction (≥ 0) ; absente = 1 (100 %).")
        BigDecimal ratio
) {
}
