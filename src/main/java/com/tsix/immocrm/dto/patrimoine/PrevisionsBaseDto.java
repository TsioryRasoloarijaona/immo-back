package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Prévisions de base de la tranche : en-tête (colonnes Base* de CfgTranche) et lignes mensuelles.")
public record PrevisionsBaseDto(
        @Schema(description = "Nombre d'articles en stock (CountItemBasePrevision).") Integer nombreArticles,
        @Schema(description = "Valorisation du stock TTC (TotalSaleBasePrevision).") BigDecimal valorisationStock,
        @Schema(description = "Pourcentage de concrétisation, fraction (BaseConcretisationPercent).")
        BigDecimal pourcentageConcretisation,
        @Schema(description = "Date début de concrétisation (BaseMarketingStartDate).") LocalDateTime debutConcretisation,
        @Schema(description = "Date fin de concrétisation (BaseEndDateConcretisation).") LocalDateTime finConcretisation,
        @Schema(description = "Date début de finalisation (BaseStartDateFinalisation).") LocalDateTime debutFinalisation,
        @Schema(description = "Date fin de finalisation (BaseMarketingEndDate).") LocalDateTime finFinalisation,
        @Schema(description = "Écart type de la concrétisation (BaseEcarttypeConcretisation) ; absent = 1.")
        BigDecimal ecartTypeConcretisation,
        @Schema(description = "Écart type de la finalisation (BaseEcarttypeFinalisation) ; absent = 1.")
        BigDecimal ecartTypeFinalisation,
        @Schema(description = "Lignes mensuelles, triées par année et mois.") List<LignePrevisionDto> lignes
) {
}
