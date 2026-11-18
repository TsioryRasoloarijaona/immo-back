package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Saisie d'une décision ; description et observation obligatoires.")
public record DecisionSaisieDto(
        @Schema(description = "O.D.T. ; absent = nombre de lignes + 1.") Integer ordre,
        @Schema(description = "Lot (PrjMarket.PKey) du projet de la réunion.") UUID lotId,
        @Schema(description = "Fournisseur (CfgTier.PKey).") UUID fournisseurId,
        @Schema(description = "Description (500 caractères au plus).", requiredMode = Schema.RequiredMode.REQUIRED)
        String description,
        @Schema(description = "Statut de réserve (BulObservationStatus.PKey).", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID observationId,
        @Schema(description = "Objectif (date).") LocalDate objectif,
        @Schema(description = "Note (500 caractères au plus).") String note,
        @Schema(description = "« Nous mêmes » ; absent = non.") Boolean nousMemes) {
}
