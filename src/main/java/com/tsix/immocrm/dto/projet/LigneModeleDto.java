package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Ligne d'un modèle de simulation (PrjMarketPrevisionModelLine), fractions 0–1.")
public record LigneModeleDto(
        @Schema(description = "Avancement du temps (PointTime), fraction de la durée du lot.") BigDecimal temps,
        @Schema(description = "Avancement des travaux cumulé (PointAchivement).") BigDecimal avancement,
        @Schema(description = "Part du paiement de la ligne (PointPayment) ; la somme des lignes vaut 1.") BigDecimal paiement
) {
}
