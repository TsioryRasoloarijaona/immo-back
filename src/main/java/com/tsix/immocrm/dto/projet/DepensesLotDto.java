package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Dépenses d'un lot du projet : factures (INVOICE) du lot, ou hors lot si lotId est null.")
public record DepensesLotDto(
        @Schema(description = "Lot de marché (PrjMarket.PKey) ; null = factures du projet sans lot (« Hors lot »).") UUID lotId,
        @Schema(description = "Code du lot ; null pour la ligne « Hors lot ».") String lotCode,
        @Schema(description = "Nombre de factures.") int nbFactures,
        @Schema(description = "Σ des montants TTC (AmountTTC).") BigDecimal montantTTC,
        @Schema(description = "Σ des montants à payer, chacun arrondi à 2 décimales.") BigDecimal montantAPayer,
        @Schema(description = "Σ des réglés des factures, règlements annulés exclus.") BigDecimal montantRegle,
        @Schema(description = "Σ à payer − Σ réglé.") BigDecimal resteARegler,
        @Schema(description = "Factures dont le reste dépasse 0,05.") int nbNonSoldees,
        @Schema(description = "Factures trop réglées (reste < −0,05, pièce à payer positive).") int nbTropReglees,
        @Schema(description = "Factures liées à un décompte (PrjWorkingOut.BuyDocumentId).") int nbFacturesDecompte) {
}
