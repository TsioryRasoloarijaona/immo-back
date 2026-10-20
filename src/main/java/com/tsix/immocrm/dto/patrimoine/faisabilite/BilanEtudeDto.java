package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Valeurs stockées par OMRAN, jamais recalculées dans ce lot (FEATURE-patrimoine-faisabilite.md §2.3, §8 bis.1). */
@Schema(description = "Bilan de l'étude en lecture seule : agrégats des prévisions tels que stockés (rendus vivants en P8b).")
public record BilanEtudeDto(
        @Schema(description = "Turnover : chiffre d'affaires TTC.") BigDecimal chiffreAffaires,
        @Schema(description = "TotalCostExecution : coût d'exécution TTC.") BigDecimal coutExecution,
        @Schema(description = "TotalCostExecutionHT : coût d'exécution HT.") BigDecimal coutExecutionHt,
        @Schema(description = "TotalCost : coût global TTC.") BigDecimal coutTotal,
        @Schema(description = "TotalCostHT : coût global HT.") BigDecimal coutTotalHt,
        @Schema(description = "UnitBackCost : coût de revient du m².") BigDecimal coutRevientM2,
        @Schema(description = "UnitSalePrice : prix de vente du m².") BigDecimal prixVenteM2,
        @Schema(description = "percentageConcretisation : part de la concrétisation (fraction).")
        BigDecimal pourcentageConcretisation,
        @Schema(description = "ConretisationAmount : montant de la concrétisation.") BigDecimal montantConcretisation,
        @Schema(description = "FinalizationAmount : montant de la finalisation.") BigDecimal montantFinalisation,
        @Schema(description = "TotalCostFunding : coût du financement.") BigDecimal coutFinancement,
        @Schema(description = "AutofinancementRate : part des fonds propres (fraction).") BigDecimal tauxAutofinancement,
        @Schema(description = "AutofinancementAmount : fonds propres.") BigDecimal montantAutofinancement,
        @Schema(description = "ClientfinancementRate : part du financement client (fraction).") BigDecimal tauxFinancementClient,
        @Schema(description = "CreditfinancementRate : part du crédit (fraction).") BigDecimal tauxFinancementCredit,
        @Schema(description = "GrossProfitMargin : marge brute.") BigDecimal margeBrute,
        @Schema(description = "ISRatio : taux de l'impôt sur les sociétés (fraction).") BigDecimal tauxIs,
        @Schema(description = "ISAmount : impôt sur les sociétés.") BigDecimal montantIs,
        @Schema(description = "NetProfitMargin : marge nette.") BigDecimal margeNette,
        @Schema(description = "ProfitRatioByProjectCost : marge / coût du projet.") BigDecimal rentabiliteCoutProjet,
        @Schema(description = "ProfitRatioByTurnover : marge / chiffre d'affaires.") BigDecimal rentabiliteChiffreAffaires,
        @Schema(description = "ProfitRatioByAutofinancementAmount : marge / fonds propres.") BigDecimal rentabiliteFondsPropres) {
}
