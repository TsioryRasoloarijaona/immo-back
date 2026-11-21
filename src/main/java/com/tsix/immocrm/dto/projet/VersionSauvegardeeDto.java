package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Version sauvegardée et message OMRAN à afficher.")
public record VersionSauvegardeeDto(
        @Schema(description = "La version créée.") PlanificationListItemDto version,
        @Schema(description = "« Version '<description>' sauvegardée correctement. »") String message) {
}
