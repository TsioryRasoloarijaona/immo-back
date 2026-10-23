package com.tsix.immocrm.dto.patrimoine.prospectionterrain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Grille complète des zones : id connu = mise à jour, sans id = création, absente = suppression.")
public record ZonesTerrainEcritureDto(List<ZoneTerrainEcritureDto> lignes) {
}
