package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Totaux de la liste des décomptes : mêmes filtres, toutes pages ; une clé par colonne de montant de la liste.")
public record TotauxDecomptesDto(
        @Schema(description = "Nombre de décomptes filtrés.") long nombre,
        @Schema(description = "Σ montantHT.") BigDecimal montantHT,
        @Schema(description = "Σ montantTTC.") BigDecimal montantTTC,
        @Schema(description = "Σ lignesTTC (Ava. TTC).") BigDecimal lignesTTC,
        @Schema(description = "Σ depassementTTC (Dépa. TTC).") BigDecimal depassementTTC,
        @Schema(description = "Σ travauxSupplementaires (TraSup. TTC).") BigDecimal travauxSupplementaires,
        @Schema(description = "Σ approvisionnementAPayer (Appro. TTC).") BigDecimal approvisionnementAPayer,
        @Schema(description = "Σ fluctuationsAPayer (Fluc. TTC).") BigDecimal fluctuationsAPayer,
        @Schema(description = "Σ totalTTC (Tot. TTC).") BigDecimal totalTTC,
        @Schema(description = "Σ retenueGarantie.") BigDecimal retenueGarantie,
        @Schema(description = "Σ netAPayer.") BigDecimal netAPayer,
        @Schema(description = "Σ montantRegle.") BigDecimal montantRegle,
        @Schema(description = "Σ resteARegler.") BigDecimal resteARegler
) {
}
