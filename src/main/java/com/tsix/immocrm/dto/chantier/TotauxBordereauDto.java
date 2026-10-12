package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Totaux du bordereau définitif : le même calcul que le montant marché du lot.")
public record TotauxBordereauDto(
        @Schema(description = "Σ quantité × PU HT.") BigDecimal totalHT,
        @Schema(description = "Σ quantité × PU HT × taux de TVA.") BigDecimal totalTVA,
        @Schema(description = "Σ quantité × PU TTC.") BigDecimal totalTTC,
        @Schema(description = "Nombre de lignes.") int nbLignes) {
}
