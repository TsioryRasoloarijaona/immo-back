package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne d'un décompte (PrjWorkingOutLine) jointe à sa ligne de bordereau.")
public record LigneDecompteDto(
        @Schema(description = "Identifiant de la ligne.") UUID id,
        @Schema(description = "Ordre de la ligne.") Integer ordre,
        @Schema(description = "N° d'article.") String code,
        @Schema(description = "Désignation.") String designation,
        @Schema(description = "Unité.") String unite,
        @Schema(description = "Quantité du marché (PrjMarketLine.Quantity).") BigDecimal quantiteMarche,
        @Schema(description = "Prix unitaire HT de la ligne de décompte.") BigDecimal prixUnitaireHT,
        @Schema(description = "Quantité cumulée des décomptes précédents (QuantityOld).") BigDecimal quantiteAnterieure,
        @Schema(description = "Quantité de la période (Quantity = QuantityOrigin × Coefficient).") BigDecimal quantitePeriode,
        @Schema(description = "Antérieure + période.") BigDecimal quantiteCumulee,
        @Schema(description = "Coefficient appliqué.") BigDecimal coefficient,
        @Schema(description = "Quantité de la période × prix unitaire HT.") BigDecimal montantHTPeriode,
        @Schema(description = "Cumulée / quantité marché × 100 ; null si quantité marché nulle ; peut dépasser 100.")
        BigDecimal avancementLignePct) {
}
