package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Point de prévision stocké (PrjMarketPrevision ou PrjMarketUserPrevision).")
public record PointSaisiDto(
        @Schema(description = "PKey du point.") UUID id,
        @Schema(description = "Date du point.") LocalDate date,
        @Schema(description = "Avancement cumulé (fraction 0–1).") BigDecimal avancement,
        @Schema(description = "Temps écoulé depuis le début (fraction 0–1 de la durée).") BigDecimal temps,
        @Schema(description = "Avancement propre au point (fraction 0–1).") BigDecimal paiement,
        @Schema(description = "Montant TTC du point : paiement × prix des prévisions ou TTC de base.") BigDecimal montant
) {
}
