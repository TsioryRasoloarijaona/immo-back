package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Terrain rattaché à la tranche (CfgTrancheStkItem).")
public record TerrainTrancheDto(
        @Schema(description = "Id de la ligne (CfgTrancheStkItem.PKey).")
        UUID id,
        @Schema(description = "Terrain (StkItem.id, nature TER).")
        UUID terrainId,
        @Schema(description = "Code du terrain, lecture seule.")
        String terrainCode,
        @Schema(description = "Libellé du terrain, lecture seule.")
        String terrainDescription,
        @Schema(description = "Superficie du terrain (StkItem.Area, m²), lecture seule.")
        BigDecimal surface,
        @Schema(description = "Quote-part du terrain affectée à la tranche, en fraction (1 = 100 %).")
        BigDecimal ratio,
        @Schema(description = "Rang de la ligne (Sorting), à partir de 0.")
        Integer ordre
) {
}
