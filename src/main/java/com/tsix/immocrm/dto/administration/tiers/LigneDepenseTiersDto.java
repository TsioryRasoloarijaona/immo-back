package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Facture payable du tiers (BuyDocument INVOICE, CanBePayed), montants tels qu'en base.")
public record LigneDepenseTiersDto(
        @Schema(description = "PKey de la facture.") UUID id,
        @Schema(description = "Code.") String code,
        @Schema(description = "Description.") String description,
        @Schema(description = "Date de la facture (DocDate), clé du tri.") LocalDateTime date,
        @Schema(description = "Échéance (MaturityDate).") LocalDateTime echeance,
        @Schema(description = "Libellé de la société.") String societe,
        @Schema(description = "Libellé du statut (BuyDocumentStatus).") String statut,
        @Schema(description = "Montant HT.") BigDecimal montantHt,
        @Schema(description = "Montant TVA.") BigDecimal montantTva,
        @Schema(description = "Montant TTC.") BigDecimal montantTtc,
        @Schema(description = "Montant à payer (AmountToPay).") BigDecimal aPayer,
        @Schema(description = "Notes.") String notes) implements LigneOngletTiers {
}
