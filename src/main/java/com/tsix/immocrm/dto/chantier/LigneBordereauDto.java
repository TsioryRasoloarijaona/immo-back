package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne du bordereau définitif (PrjMarketLine) ; les totaux sont recalculés, pas stockés.")
public record LigneBordereauDto(
        @Schema(description = "Identifiant de la ligne.") UUID id,
        @Schema(description = "Ordre de la ligne (Sorting).") Integer ordre,
        @Schema(description = "Libellé du groupe de lignes (PrjMarketLineGrouping) ; null pour la plupart des lots.")
        String groupe,
        @Schema(description = "N° d'article.") String code,
        @Schema(description = "Désignation.") String designation,
        @Schema(description = "Unité.", example = "M3") String unite,
        @Schema(description = "Quantité du marché.") BigDecimal quantite,
        @Schema(description = "Prix unitaire HT (16 décimales en base).") BigDecimal prixUnitaireHT,
        @Schema(description = "Taux de TVA, pourcentage 0–100.", example = "20") BigDecimal tauxTVA,
        @Schema(description = "Quantité × prix unitaire HT.") BigDecimal totalHT,
        @Schema(description = "Quantité × prix unitaire TTC.") BigDecimal totalTTC) {
}
