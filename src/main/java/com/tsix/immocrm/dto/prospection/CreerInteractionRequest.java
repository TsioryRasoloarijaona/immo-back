package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Corps de création d'une interaction (appel, message, visite...) sur une "
        + "prospection ou une fiche.")
public record CreerInteractionRequest(

        // Nommé `description` alors qu'il alimente `note` en sortie : asymétrie assumée, voir
        // instructions/FIX-interaction-note.md §6.
        @Schema(example = "la cliente cherche 2 chambres et salon, rdv mercredi 10h")
        String description,

        Boolean entrante,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UUID actionTypeId,

        UUID resultatId,

        LocalDateTime dateAlerte,

        LocalDateTime dateRdv
) {
}
