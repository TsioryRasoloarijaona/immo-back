package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Décision d'une réunion (BulMeetingLine).")
public record DecisionDto(
        @Schema(description = "BulMeetingLine.PKey.") UUID id,
        @Schema(description = "O.D.T.") Integer ordre,
        @Schema(description = "Lot (PrjMarket.PKey) ; null si aucun.") UUID lotId,
        @Schema(description = "Libellé du lot.") String lot,
        @Schema(description = "Fournisseur (CfgTier.PKey) ; null si aucun.") UUID fournisseurId,
        @Schema(description = "Raison sociale du fournisseur.") String fournisseur,
        @Schema(description = "Description de la décision.") String description,
        @Schema(description = "Observation : statut de réserve choisi.") ObservationDto observation,
        @Schema(description = "Objectif (BulMeetingLine.DueDate) ; null si aucun.") LocalDate objectif,
        @Schema(description = "Note.") String note,
        @Schema(description = "« Nous mêmes » (IsInternal) : la décision nous incombe.") boolean nousMemes,
        @Schema(description = "Calculé : statut de réserve fermé (BulObservationStatus.IsColsed), jamais stocké.")
        boolean fermee) {
}
