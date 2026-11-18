package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Saisie d'un intervenant ; un seul intervenant par couple (lot, fournisseur) dans une réunion.")
public record IntervenantSaisieDto(
        @Schema(description = "O.D.T. ; absent = nombre de lignes + 1.") Integer ordre,
        @Schema(description = "Lot (PrjMarket.PKey) du projet de la réunion.") UUID lotId,
        @Schema(description = "Fournisseur (CfgTier.PKey).") UUID fournisseurId,
        @Schema(description = "Effectif.") Integer effectif,
        @Schema(description = "Note (500 caractères au plus).") String note) {
}
