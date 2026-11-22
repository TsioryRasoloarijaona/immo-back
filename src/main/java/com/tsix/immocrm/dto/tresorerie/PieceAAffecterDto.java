package com.tsix.immocrm.dto.tresorerie;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Pièce d'achat affectable : visible, du fournisseur et de la société demandés, non soldée.")
public record PieceAAffecterDto(
        @Schema(description = "BuyDocument.PKey.") UUID id,
        @Schema(description = "Code de la pièce.") String code,
        @Schema(description = "Date de la pièce.") LocalDate dateDocument,
        @Schema(description = "Date d'échéance de la pièce.") LocalDate dateEcheance,
        @Schema(description = "Description de la pièce.") String description,
        @Schema(description = "Net à payer de la pièce.") BigDecimal netAPayer,
        @Schema(description = "Déjà réglé, hors règlements annulés et hors le règlement en cours de modification.")
        BigDecimal regle,
        @Schema(description = "Reste à payer = net à payer − réglé, > 0.") BigDecimal resteAPayer,
        @Schema(description = "Code de la devise de la pièce.", example = "MAD") String deviseCode) {
}
