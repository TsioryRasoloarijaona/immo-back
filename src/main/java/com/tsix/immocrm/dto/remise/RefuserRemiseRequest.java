package com.tsix.immocrm.dto.remise;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Refus d'une demande de remise par le siège.")
public record RefuserRemiseRequest(
        @Schema(description = "Motif du refus, obligatoire, 1 000 caractères au plus ; visible de l'agence.")
        String motif
) {
}
