package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne de liste d'un modèle ou d'une version de planification (CfgGanttProject).")
public record PlanificationListItemDto(
        @Schema(description = "Id de la planification (PKey).") UUID id,
        @Schema(description = "Code unique.") String code,
        @Schema(description = "Description ; pour une version, sa date courte.") String description,
        @Schema(description = "Notes (1 000 caractères au plus).") String notes,
        @Schema(description = "Date de début de la planification (StartDate).") LocalDate dateDebut,
        @Schema(description = "Date et heure de la version ; null pour un modèle ou la planification d'un projet.")
        LocalDateTime dateVersion) {
}
