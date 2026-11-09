package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Travail supplémentaire d'un décompte (PrjWorkingOutLineExtra).")
public record ExtraDecompteDto(
        @Schema(description = "PrjWorkingOutLineExtra.Pkey.") UUID id,
        @Schema(description = "Désignation.") String designation,
        @Schema(description = "Unité.") String unite,
        @Schema(description = "Quantité.") BigDecimal quantite,
        @Schema(description = "Prix unitaire HT.") BigDecimal prixUnitaireHT,
        @Schema(description = "Taux de TVA en pourcentage.") BigDecimal tauxTVA,
        @Schema(description = "Quantité × prix unitaire HT.") BigDecimal montantHT,
        @Schema(description = "Quantité × prix unitaire TTC.") BigDecimal montantTTC
) {
}
