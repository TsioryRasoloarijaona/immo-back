package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fenêtre de saisie des prévisions d'un lot (OMRAN ProjectPrevisionMarket).")
public record PrevisionsSaisieLotDto(
        @Schema(description = "Lot (PrjMarket.PKey).") UUID lotId,
        @Schema(description = "Libellé du lot.") String description,
        @Schema(description = "Vrai si le compte peut enregistrer (droit ProjectPrevision.toolNew).") boolean modifiable,
        @Schema(description = "Vrai si l'estimation de base est modifiable (droit ManualEstimationAdministrator).") boolean estimationDeBaseModifiable,
        @Schema(description = "Début des prévisions (StartDate).") LocalDate debut,
        @Schema(description = "Durée des prévisions en jours (Delay).") Integer duree,
        @Schema(description = "Fin des prévisions = début + durée.") LocalDate fin,
        @Schema(description = "Début de l'estimation de base (StartDateManual).") LocalDate debutBase,
        @Schema(description = "Durée de l'estimation de base en jours (DelayManual).") Integer dureeBase,
        @Schema(description = "Fin de l'estimation de base = début + durée.") LocalDate finBase,
        @Schema(description = "Estimation de base HT (EstimatedPriceHTManual).") BigDecimal estimationBaseHT,
        @Schema(description = "Estimation de base TVA (EstimatedVatAmountManual).") BigDecimal estimationBaseTVA,
        @Schema(description = "Estimation de base TTC (EstimatedPriceTTCManual).") BigDecimal estimationBaseTTC,
        @Schema(description = "Taux de TVA par défaut du lot en pourcentage (DefaultVat × 100).") BigDecimal tvaParDefaut,
        @Schema(description = "Estimation confidentielle HT (lignes estimatives).") BigDecimal estimationConfidentielleHT,
        @Schema(description = "Estimation confidentielle TVA.") BigDecimal estimationConfidentielleTVA,
        @Schema(description = "Estimation confidentielle TTC.") BigDecimal estimationConfidentielleTTC,
        @Schema(description = "Marché HT (bordereau ou devis retenu).") BigDecimal marcheHT,
        @Schema(description = "Marché TVA.") BigDecimal marcheTVA,
        @Schema(description = "Marché TTC.") BigDecimal marcheTTC,
        @Schema(description = "TTC qui valorise les points « Prévisions » : marché si non nul, sinon estimation confidentielle.") BigDecimal prixPrevisions,
        @Schema(description = "Avancement facturé à date (fraction 0–1), lecture seule.") BigDecimal avancementReel,
        @Schema(description = "Points « Prévisions » stockés, par date ; vide si aucun.") List<PointSaisiDto> points,
        @Schema(description = "Points « Estimation de base » stockés, par date ; vide si aucun.") List<PointSaisiDto> pointsBase
) {
}
