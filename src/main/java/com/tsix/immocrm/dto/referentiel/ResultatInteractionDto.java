package com.tsix.immocrm.dto.referentiel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** {@code ComActionMessageSubCategory}. */
public record ResultatInteractionDto(

        UUID id,

        @Schema(description = "Libellé du résultat, ex: \"Rendez vous pris\", \"Hors Budget\".")
        String code,

        String description,

        boolean necessiteDateRdv,

        boolean alerteRecommandee
) {
}
