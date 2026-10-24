package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Prospection terrain à lier à l'étude.")
public record LierProspectionEtudeDto(
        @Schema(description = "Prospection terrain (LndLand.PKey), obligatoire.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID prospectionTerrainId) {
}
