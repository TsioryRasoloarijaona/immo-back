package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Pièce à payer du lot (avance ou remboursement) ; réglé et reste = solde partagé de la liste Achat.")
public record AvanceLotDto(
        @Schema(description = "BuyDocument.PKey.") UUID id,
        @Schema(description = "Code de la pièce.") String code,
        @Schema(description = "Statut de la pièce (BuyDocumentStatus.PKey), null si aucun.") UUID statutId,
        @Schema(description = "Libellé du statut.") String statutDescription,
        @Schema(description = "Couleur du statut (nom .NET, ColorName).", example = "Orange") String statutCouleur,
        @Schema(description = "Date de la pièce (DocDate).") LocalDate date,
        @Schema(description = "Net à payer (AmountToPay) ; négatif = avance à déduire.") BigDecimal netAPayer,
        @Schema(description = "Σ des règlements non annulés.") BigDecimal regle,
        @Schema(description = "Net à payer − réglé ; négatif possible.") BigDecimal reste,
        @Schema(description = "Code de la devise (StlCurrency.Code).") String deviseCode,
        @Schema(description = "Notes de la pièce.") String notes
) {
}
