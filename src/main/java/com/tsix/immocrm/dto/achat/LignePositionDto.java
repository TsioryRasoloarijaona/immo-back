package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Ligne de la fenêtre « Ligne de solde » d'OMRAN : une pièce ou une part de règlement du fournisseur.")
public record LignePositionDto(
        @Schema(description = "Pièce (BuyDocument.id) ou règlement (StlSettlement.id).") UUID id,
        String type,
        @Schema(description = "Code de la pièce ou du règlement.") String code,
        @Schema(description = "Référence, description et notes, séparées par un espace.") String description,
        @Schema(description = "Date de la pièce ou du règlement (DocDate).") LocalDate dateDocument,
        LocalDate dateEcheance,
        @Schema(description = "Projet de la pièce (ou de la pièce réglée) ; null pour une part non rattachée.") UUID projetId,
        @Schema(description = "Code du projet (PrjProject.Code).") String projetCode,
        BigDecimal montantTTC,
        @Schema(description = "Pièce : aPayer − montantTTC ; null pour un règlement.") BigDecimal retenues,
        BigDecimal aPayer,
        @Schema(description = "Règlement : montant de base (AmountBase) de la part ; null pour une pièce.") BigDecimal base,
        @Schema(description = "Règlement : retenue à la source (AmountRS) de la part ; null pour une pièce.") BigDecimal ras,
        @Schema(description = "Règlement : montant de la part, au débit ou au crédit ; null pour une pièce.") BigDecimal reglement,
        @Schema(description = "Vrai : au crédit du fournisseur (augmente le solde) ; faux : au débit.") boolean credit) {
}
