package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Ligne de l'onglet Terrains (CfgTrancheStkItem).")
public record LigneTerrainDto(
        @Schema(description = "PKey de la ligne ; absent au PUT = nouvelle ligne.") UUID id,
        @Schema(description = "Terrain (StkItem), obligatoire ; choix : /api/tranches/terrains-disponibles.") UUID terrainId,
        @Schema(description = "Ratio (fraction, affichée en %) ; absent = 1. Aucun contrôle de Σ.") BigDecimal ratio,
        @Schema(description = "Rang (Sorting), recalculé par le serveur selon l'ordre de la liste.", accessMode = READ_ONLY)
        Integer ordre,
        @Schema(description = "Code du terrain.", accessMode = READ_ONLY) String terrainCode,
        @Schema(description = "Description du terrain.", accessMode = READ_ONLY) String terrainDescription,
        @Schema(description = "Superficie du terrain (StkItem.Area), m².", accessMode = READ_ONLY) BigDecimal terrainSurface
) implements LignesTrancheDto.Ligne {
}
