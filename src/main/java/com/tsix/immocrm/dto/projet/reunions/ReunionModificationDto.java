package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Modification de l'en-tête d'une réunion ; projet et type ne sont pas modifiables.")
public record ReunionModificationDto(
        @Schema(description = "Référence P.V. ; vide = générée « yy/nnnnn ».") String reference,
        @Schema(description = "Date de la réunion, obligatoire.", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(description = "Date de la prochaine réunion, postérieure à la date.") LocalDate prochaineDate,
        @Schema(description = "Surveillant (CfgTier.PKey).") UUID surveillantId,
        @Schema(description = "Note (500 caractères au plus).") String note) {
}
