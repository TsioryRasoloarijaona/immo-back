package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Ligne de la liste « Réunions chantier » (BulMeeting).")
public record ReunionLigneDto(
        @Schema(description = "BulMeeting.PKey.") UUID id,
        @Schema(description = "Libellé du projet (PrjProject.Description, à défaut son code).") String projet,
        @Schema(description = "Libellé du type de réunion (BulMeetingType.Description).") String type,
        @Schema(description = "Référence P.V. (BulMeeting.Code).", example = "26/00002") String reference,
        @Schema(description = "Date de la réunion (jour).") LocalDate date,
        @Schema(description = "Date de la prochaine réunion ; null si non prévue.") LocalDate prochaineDate,
        @Schema(description = "Raison sociale du surveillant (CfgTier) ; null si aucun.") String surveillant,
        @Schema(description = "Note libre.") String note,
        @Schema(description = "O.D.T. : rang de la réunion dans son projet et son type.") Integer ordre) {
}
