package com.tsix.immocrm.dto.dossier.visites;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Programmation d'une visite technique pour le dossier.")
public record ProgrammerVisiteRequest(
        @Schema(description = "Date et heure de début de la visite. Requise.")
        LocalDateTime debut,
        @Schema(description = "Date et heure de fin, postérieure au début. Requise.")
        LocalDateTime fin,
        @Schema(description = "Note libre.")
        String note
) {
}
