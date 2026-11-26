package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Pièce d'achat générée depuis un décompte : la facture fournisseur ou l'avoir.")
public record FactureGenereeDto(
        @Schema(description = "Identifiant de la pièce créée (BuyDocument.PKey).") UUID factureId,
        @Schema(description = "Code de la pièce, tiré de sa séquence (« Facture fournisseur » ou « Avoir fournisseur »).")
        String code,
        @Schema(description = "Net à payer de la pièce (BuyDocument.AmountToPay), calculé par le serveur.") BigDecimal netAPayer) {
}
