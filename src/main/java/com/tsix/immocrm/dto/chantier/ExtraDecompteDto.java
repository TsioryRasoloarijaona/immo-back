package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Travaux supplémentaires d'un décompte (PrjWorkingOutLineExtra).")
public record ExtraDecompteDto(
        @Schema(description = "Identifiant.") UUID id,
        @Schema(description = "Code.") String code,
        @Schema(description = "Désignation.") String designation,
        @Schema(description = "Unité.") String unite,
        @Schema(description = "Quantité.") BigDecimal quantite,
        @Schema(description = "Prix unitaire HT.") BigDecimal prixUnitaire,
        @Schema(description = "Taux de TVA, pourcentage 0–100.") BigDecimal tauxTVA,
        @Schema(description = "Quantité × PU HT.") BigDecimal montantHT,
        @Schema(description = "Quantité × PU TTC (Σ = extraAmount du décompte).") BigDecimal montantTTC) {
}
