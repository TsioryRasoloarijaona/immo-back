package com.tsix.immocrm.dto.patrimoine.prospectionterrain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Terrain créé par la transformation (fiche : GET /api/terrains/{terrainId}).")
public record TerrainCreeDto(@Schema(description = "StkItem.PKey.") UUID terrainId, String code) {
}
