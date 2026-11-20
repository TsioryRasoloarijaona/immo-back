package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Intervenant d'une planification (CfgGanttOwner) ; en saisie, un champ absent reste inchangé.")
public record IntervenantPlanificationDto(
        @Schema(description = "Id de l'intervenant (PKey) ; ignoré en saisie.") UUID id,
        @Schema(description = "Code, obligatoire à la création.") String code,
        @Schema(description = "Description.") String description,
        @Schema(description = "Notes.") String notes,
        @Schema(description = "Visible dans le diagramme ; défaut vrai.") Boolean visible,
        @Schema(description = "Tiers associé (CfgTier.PKey), facultatif.") UUID tiersId) {
}
