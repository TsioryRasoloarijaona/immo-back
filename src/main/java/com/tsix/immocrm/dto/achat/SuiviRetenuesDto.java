package com.tsix.immocrm.dto.achat;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Suivi des retenues de garantie : lignes de la période et totaux hors annulées.")
public record SuiviRetenuesDto(List<LigneSuiviRetenueDto> lignes, TotauxSuiviRetenuesDto totaux) {

    @Schema(description = "Ligne du suivi des retenues de garantie : colonnes de la pièce et trois montants.")
    public record LigneSuiviRetenueDto(
            @JsonUnwrapped PieceSuiviAchatDto piece,
            @Schema(description = "Retenue de garantie (WarrantyAmount) de la pièce, négative pour un avoir.") BigDecimal retenue,
            @Schema(description = "Pénalité : régularisation (AmountRegul) d'une restitution, hors cumul de retenue.")
            BigDecimal penalite,
            @Schema(description = "Remboursé : à payer (AmountToPay) d'une pièce de restitution, 0 sinon.") BigDecimal rembourse) {
    }

    @Schema(description = "Totaux du suivi des retenues de garantie, pièces annulées exclues.")
    public record TotauxSuiviRetenuesDto(
            @Schema(description = "Σ des retenues.") BigDecimal retenue,
            @Schema(description = "Σ des pénalités.") BigDecimal penalite,
            @Schema(description = "Σ des remboursements.") BigDecimal rembourse) {
    }
}
