package com.tsix.immocrm.dto.kpi;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nombre d'actions d'un type (ComActionMessageType) sur la période.")
public record ActionParTypeDto(
        @Schema(example = "Téléphone Sortant")
        String type,
        @Schema(description = "Nombre d'actions de ce type.", example = "312")
        long nombre
) {
}
