package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne d'approvisionnement d'un décompte (PrjWorkingOutStockLine).")
public record LigneApprovisionnementDto(
        @Schema(description = "PrjWorkingOutStockLine.Pkey.") UUID id,
        @Schema(description = "Ordre (Sorting).") Integer ordre,
        @Schema(description = "Code.") String code,
        @Schema(description = "Désignation.") String designation,
        @Schema(description = "Unité.") String unite,
        @Schema(description = "Quantité.") BigDecimal quantite,
        @Schema(description = "Prix unitaire HT.") BigDecimal prixUnitaireHT,
        @Schema(description = "Taux de TVA en pourcentage (VatRatio × 100).") BigDecimal tauxTVA,
        @Schema(description = "Quantité × prix HT × (1 + FODEC), 2 décimales.") BigDecimal montantHT,
        @Schema(description = "Montant HT × (1 + TVA), 2 décimales.") BigDecimal montantTTC
) {
}
