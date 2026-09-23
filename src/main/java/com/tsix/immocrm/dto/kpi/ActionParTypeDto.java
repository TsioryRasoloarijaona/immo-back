package com.tsix.immocrm.dto.kpi;

import io.swagger.v3.oas.annotations.media.Schema;

/** Nombre d'actions d'un type donné, pour un commercial et une période. */
@Schema(description = "Nombre d'actions d'un type (ComActionMessageType) sur la période.")
public record ActionParTypeDto(
        @Schema(description = "Libellé du type, tel que le référentiel le porte. « (sans type) » pour "
                + "les actions sans type (9 sur 12 mois en base réelle).", example = "Téléphone Sortant")
        String type,
        @Schema(description = "Nombre d'actions de ce type.", example = "312")
        long nombre
) {
}
