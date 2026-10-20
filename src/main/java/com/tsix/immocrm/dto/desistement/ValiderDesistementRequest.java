package com.tsix.immocrm.dto.desistement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Validation d'un désistement : passe le dossier en DESISTEMENT et génère ses pièces.")
public record ValiderDesistementRequest(
        @Schema(description = "Date de validation (`ValidityDate`).", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime dateValidation,
        @Schema(description = "Date de légalisation (`LegalizationDate`), obligatoire si la pénalité après remise n'est pas nulle.")
        LocalDateTime dateLegalisation
) {
}
