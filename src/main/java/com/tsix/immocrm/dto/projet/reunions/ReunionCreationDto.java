package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Création d'une réunion ; le P.V. précédent du même projet et type est reporté.")
public record ReunionCreationDto(
        @Schema(description = "Projet (PrjProject.PKey), obligatoire.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID projetId,
        @Schema(description = "Type de réunion (BulMeetingType.PKey), obligatoire.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID typeId,
        @Schema(description = "Référence P.V. ; vide = générée « yy/nnnnn ».") String reference,
        @Schema(description = "Date de la réunion, obligatoire.", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(description = "Date de la prochaine réunion, postérieure à la date.") LocalDate prochaineDate,
        @Schema(description = "Surveillant (CfgTier.PKey).") UUID surveillantId,
        @Schema(description = "Note (500 caractères au plus).") String note) {

    public ReunionModificationDto enTete() {
        return new ReunionModificationDto(reference, date, prochaineDate, surveillantId, note);
    }
}
