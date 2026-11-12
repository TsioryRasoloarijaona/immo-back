package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Retenues et avances de la pièce (colonne droite OMRAN), lues telles quelles ; null si non renseignées.")
public record RetenuesAvancesAchatDto(
        @Schema(description = "Amortissement d'avance (AdvanceRatio, AdvanceAmount).") RetenueAchatDto amortissementAvance,
        @Schema(description = "Reste de l'avance à amortir (AdvanceRest).") BigDecimal resteAvance,
        @Schema(description = "Retenue de garantie (WarrantyRatio, WarrantyAmount).") RetenueAchatDto retenueGarantie,
        @Schema(description = "Retenue assurance TRC (InsuranceTRCRatio, InsuranceTRCAmount).") RetenueAchatDto assuranceTRC,
        @Schema(description = "Retenue assurance décennale (InsuranceDECRatio, InsuranceDECAmount).")
        RetenueAchatDto assuranceDecennale,
        @Schema(description = "Retenue compte prorata (ProportionRatio, ProportionAmount).") RetenueAchatDto compteProrata,
        @Schema(description = "Retenues forfaitaires (RetainedAmount).") BigDecimal retenuesForfaitaires,
        @Schema(description = "Avance sur décompte (WorkingOutAdvanceAmount).") BigDecimal avanceSurDecompte,
        @Schema(description = "Approvisionnement (StockAmountToPay).") BigDecimal approvisionnement,
        @Schema(description = "Remboursement de compte prorata (ProrataRefundAmount).") BigDecimal remboursementProrata,
        @Schema(description = "Remboursement de fluctuations (FluctuationsToPay).") BigDecimal remboursementFluctuations) {

    @Schema(description = "Retenue affichée en % et en montant.")
    public record RetenueAchatDto(
            @Schema(description = "Taux, fraction (0,07 = 7 %).", example = "0.07") BigDecimal ratio,
            @Schema(description = "Montant.") BigDecimal montant) {
    }
}
