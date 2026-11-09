package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Facture d'un décompte (BuyDocument pointée par PrjWorkingOut.BuyDocumentId).")
public record FactureDecompteDto(
        @Schema(description = "BuyDocument.PKey.") UUID id,
        @Schema(description = "Code de la facture.", example = "26/03017") String code,
        @Schema(description = "Date de la facture (DocDate).") LocalDate dateDocument,
        @Schema(description = "Date d'échéance (MaturityDate).") LocalDate dateEcheance,
        @Schema(description = "Montant HT.") BigDecimal montantHT,
        @Schema(description = "Montant TTC.") BigDecimal montantTTC,
        @Schema(description = "À payer (AmountToPay) = net du décompte + régularisation ; base du reste à régler.")
        BigDecimal montantAPayer,
        @Schema(description = "Régularisation (AmountRegul).") BigDecimal regularisation,
        @Schema(description = "Code du statut de la facture.", example = "VALIDÉ DG") String statutCode,
        @Schema(description = "Libellé du statut de la facture.") String statutDescription
) {
}
