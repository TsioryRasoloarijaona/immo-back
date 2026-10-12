package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ligne voulue de l'onglet Étages.")
public record EtageTrancheEcritureDto(
        @Schema(description = "Id d'une ligne existante de cette tranche ; absent = nouvelle ligne.")
        UUID id,
        @Schema(description = "Niveau, voir GET /api/referentiels-patrimoine/niveaux-terrain. Obligatoire.")
        UUID niveauTerrainId
) {
}
