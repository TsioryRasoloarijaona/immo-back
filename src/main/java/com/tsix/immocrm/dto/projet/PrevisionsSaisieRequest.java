package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Saisie des prévisions d'un lot ; les listes remplacent les points stockés (vides acceptées).")
public record PrevisionsSaisieRequest(
        @Schema(description = "Début des prévisions (PrjMarket.StartDate), obligatoire.") LocalDate debut,
        @Schema(description = "Durée des prévisions en jours (PrjMarket.Delay), obligatoire, ≥ 0.") Integer duree,
        @Schema(description = "Début de l'estimation de base (StartDateManual), obligatoire.") LocalDate debutBase,
        @Schema(description = "Durée de l'estimation de base en jours (DelayManual), obligatoire, ≥ 0.") Integer dureeBase,
        @Schema(description = "Estimation de base HT ; TVA et TTC sont déduites du taux par défaut du lot.") BigDecimal estimationBaseHT,
        @Schema(description = "Points « Prévisions » (PrjMarketPrevision).") List<PointSaisieDto> points,
        @Schema(description = "Points « Estimation de base » (PrjMarketUserPrevision).") List<PointSaisieDto> pointsBase
) {
}
