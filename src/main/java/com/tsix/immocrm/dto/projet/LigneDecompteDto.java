package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne de décompte (PrjWorkingOutLine) jointe à sa ligne de bordereau.")
public record LigneDecompteDto(
        @Schema(description = "PrjWorkingOutLine.Pkey ; null dans l'aperçu « lignes rechargées » (ligne pas encore écrite).") UUID id,
        @Schema(description = "Ordre (Sorting).") Integer ordre,
        @Schema(description = "Désignation.") String designation,
        @Schema(description = "Unité.") String unite,
        @Schema(description = "Quantité du marché (PrjMarketLine.Quantity).") BigDecimal quantiteMarche,
        @Schema(description = "Prix unitaire HT de la ligne de décompte.") BigDecimal prixUnitaireHT,
        @Schema(description = "Quantité des décomptes précédents (QuantityOld).") BigDecimal quantiteAnterieure,
        @Schema(description = "Quantité de la période (Quantity = (saisie + antérieure) × coefficient − antérieure).")
        BigDecimal quantitePeriode,
        @Schema(description = "Antérieure + période.") BigDecimal quantiteCumulee,
        @Schema(description = "Coefficient.") BigDecimal coefficient,
        @Schema(description = "Quantité de la période × prix unitaire HT.") BigDecimal montantHTPeriode,
        @Schema(description = "Cumulée / quantité marché × 100, 2 décimales ; null si marché nul ; peut dépasser 100.")
        BigDecimal avancementLignePct,
        @Schema(description = "Ligne de bordereau (PrjMarketLine.PKey).") UUID ligneMarcheId,
        @Schema(description = "Quantité saisie (QuantityOrigin) : seule quantité lue par l'enregistrement.") BigDecimal quantiteSaisie
) {
}
