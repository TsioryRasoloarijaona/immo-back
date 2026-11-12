package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Prévisions du lot : séries mensuelles du graphe OMRAN (estimation de base, prévision, réel).")
public record PrevisionsLotDto(
        @Schema(description = "Lot (PrjMarket.PKey).") UUID lotId,
        @Schema(description = "Premier mois de la série ; null sans aucun point.") LocalDate debut,
        @Schema(description = "Tous les mois consécutifs ; vide si le lot n'a aucun point.") List<PointPrevisionDto> mois
) {

    @Schema(description = "Mois du graphe ; montants à 0 et avancements null sans donnée.")
    public record PointPrevisionDto(
            @Schema(description = "Premier jour du mois.") LocalDate mois,
            @Schema(description = "Estimation de base : montant du mois.") BigDecimal estimation,
            @Schema(description = "Estimation de base : avancement (fraction 0–1).") BigDecimal avancementEstimation,
            @Schema(description = "Σ PrjMarketPaymentPrevision.Amount du mois.") BigDecimal prevision,
            @Schema(description = "Avancement prévu (fraction 0–1), maximum du mois ; null si négatif.") BigDecimal avancementPrevision,
            @Schema(description = "Σ net à payer des pièces du lot hors avoirs, par mois d'échéance.") BigDecimal reel,
            @Schema(description = "Avancement des décomptes facturés du mois (fraction 0–1).") BigDecimal avancementReel
    ) {
    }
}
