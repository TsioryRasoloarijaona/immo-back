package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Avenant du lot (PrjClause).")
public record AvenantLotDto(
        @Schema(description = "PrjClause.PKey.") UUID id,
        @Schema(description = "Ordre (Sorting).") Integer ordre,
        @Schema(description = "Date de l'avenant (DocDate).") LocalDate date,
        @Schema(description = "Code de l'avenant.") String code,
        @Schema(description = "Description.") String description,
        @Schema(description = "Montant HT.") BigDecimal montantHT,
        @Schema(description = "Montant de TVA.") BigDecimal montantTVA,
        @Schema(description = "Montant TTC.") BigDecimal montantTTC,
        @Schema(description = "Net à payer (AmountToPay).") BigDecimal netAPayer,
        @Schema(description = "Codes des factures de l'avenant séparés par « , » ; null sans facture.") String factureCode
) {
}
