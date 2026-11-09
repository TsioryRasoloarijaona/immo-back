package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne du bordereau définitif d'un lot (PrjMarketLine hors stock) ; totaux calculés, non stockés.")
public record LigneBordereauDto(
        @Schema(description = "PrjMarketLine.PKey.") UUID id,
        @Schema(description = "Ordre de la ligne (Sorting).") Integer ordre,
        @Schema(description = "Libellé du regroupement (PrjMarketLineGrouping), null si la ligne n'en a pas.") String groupe,
        @Schema(description = "Code de la ligne.") String code,
        @Schema(description = "Désignation.") String designation,
        @Schema(description = "Unité (Unity).") String unite,
        @Schema(description = "Quantité.") BigDecimal quantite,
        @Schema(description = "Prix unitaire HT (16 décimales en base).") BigDecimal prixUnitaireHT,
        @Schema(description = "Taux de TVA en pourcentage (VatRatio × 100).", example = "20") BigDecimal tauxTVA,
        @Schema(description = "Quantité × prix unitaire HT.") BigDecimal totalHT,
        @Schema(description = "Quantité × prix unitaire TTC.") BigDecimal totalTTC
) {
}
