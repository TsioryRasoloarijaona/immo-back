package com.tsix.immocrm.dto.dossier.visites;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Créneau de visite technique pris sur la tranche (AdmEvent rattaché à un dossier).")
public record VisiteTechniqueDto(
        @Schema(description = "Identifiant du créneau (AdmEvent.PKey).")
        UUID id,
        @Schema(description = "Dossier visité (AdmEvent.ComFolderId).")
        UUID dossierId,
        @Schema(description = "N° du dossier visité.")
        String dossierCode,
        @Schema(description = "Titre du créneau (AdmEvent.Subject).")
        String titre,
        @Schema(description = "Début de la visite (AdmEvent.StartHour).")
        LocalDateTime debut,
        @Schema(description = "Fin de la visite (AdmEvent.EndHour).")
        LocalDateTime fin,
        @Schema(description = "Note libre (AdmEvent.Notes).")
        String note
) {
}
