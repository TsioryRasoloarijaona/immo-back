package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Totaux des racines « Dépense » et « Recette » du bilan.")
public record TotauxPrevisionsDto(
        @Schema(description = "Total HT de la racine Dépense (coût global HT).") BigDecimal depenseHt,
        @Schema(description = "Total TTC de la racine Dépense (coût global TTC).") BigDecimal depense,
        @Schema(description = "Total HT de la racine Recette (chiffre d'affaires HT).") BigDecimal recetteHt,
        @Schema(description = "Total TTC de la racine Recette (chiffre d'affaires TTC).") BigDecimal recette,
        @Schema(description = "Vrai si la relation « prix du terrain » n'a pas de catégorie : terrain non inclus au coût.")
        boolean terrainNonInclus) {
}
