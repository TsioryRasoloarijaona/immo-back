package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Pré-prospections ouvertes et sans commercial des projets de l'appelant. "
        + "La liste elle-même : `GET /api/pre-prospections`.")
public record ADistribuerDto(

        @Schema(description = "Nombre de pré-prospections en attente de commercial.", example = "152")
        long nombre,

        @Schema(description = "Date (`DocDate`) de la plus ancienne. `null` si aucune.")
        LocalDateTime plusAncienneDate,

        @Schema(description = "Âge de la plus ancienne en jours, à aujourd'hui. `null` si aucune.", example = "9")
        Long agePlusAncienneJours
) {
}
