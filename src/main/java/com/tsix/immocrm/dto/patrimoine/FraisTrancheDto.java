package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Frais de vente de la tranche (CfgTrancheSettlementCategory).")
public record FraisTrancheDto(
        @Schema(description = "Id de la ligne (CfgTrancheSettlementCategory.PKey).")
        UUID id,
        @Schema(description = "Catégorie de règlement du frais (ComSettlementCategory.id).")
        UUID categorieReglementId,
        @Schema(description = "Code de la catégorie, lecture seule.")
        String categorieCode,
        @Schema(description = "Libellé de la catégorie, lecture seule.")
        String categorieDescription,
        @Schema(description = "Ordre (« O.D.T. » d'OMRAN, Sorting).")
        Integer ordre,
        @Schema(description = "Montant HT.")
        BigDecimal montantHT,
        @Schema(description = "Taux de TVA en fraction (0,2 = 20 %).")
        BigDecimal tauxTva,
        @Schema(description = "Montant TTC = HT × (1 + TVA).")
        BigDecimal montantTTC,
        @Schema(description = "Pourcentage du prix de vente (PercentOf), valeur stockée telle quelle.")
        BigDecimal pourcentageDe
) {
}
