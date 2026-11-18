package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne de l'onglet Lots : montants de base du lot, Σ de sa série générée (« calculée »), différence.")
public record LotPrevisionProjetDto(
        @Schema(description = "Lot (PrjMarket.PKey).") UUID lotId,
        @Schema(description = "Code du lot.") String code,
        @Schema(description = "Description du lot.") String description,
        @Schema(description = "Nom du fournisseur ; null sans fournisseur.") String fournisseur,
        @Schema(description = "Lot multi-fournisseurs (exclu des poids d'avancement).") boolean multiFournisseur,
        @Schema(description = "Notes du lot.") String notes,
        @Schema(description = "Lot clôturé (IsClosed).") boolean cloture,
        @Schema(description = "Lot résilié ou annulé (statut).") boolean resilie,
        @Schema(description = "Prévision net à payer (PrevisionAmountToPay).") BigDecimal previsionNetAPayer,
        @Schema(description = "Σ de l'estimation de base simulée du lot.") BigDecimal previsionNetAPayerCalculee,
        @Schema(description = "previsionNetAPayer − previsionNetAPayerCalculee.") BigDecimal previsionNetAPayerDifference,
        @Schema(description = "Coût prévisionnel net à payer (CostToPay, même valeur que la liste des lots).")
        BigDecimal coutPrevisionnelNetAPayer,
        @Schema(description = "Σ prévisions de paiement datées + Σ réel du lot.") BigDecimal coutPrevisionnelNetAPayerCalcule,
        @Schema(description = "coutPrevisionnelNetAPayer − coutPrevisionnelNetAPayerCalcule.")
        BigDecimal coutPrevisionnelNetAPayerDifference,
        @Schema(description = "Coût réel net à payer (PositionAmountToPay).") BigDecimal positionNetAPayer,
        @Schema(description = "Σ réel du lot (pièces payables hors avoirs).") BigDecimal positionNetAPayerCalculee,
        @Schema(description = "positionNetAPayer − positionNetAPayerCalculee.") BigDecimal positionNetAPayerDifference
) {
}
