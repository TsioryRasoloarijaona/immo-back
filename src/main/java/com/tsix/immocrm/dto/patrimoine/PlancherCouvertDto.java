package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Superficie couverte d'une catégorie d'article dans la tranche (CfgTrancheCoveredFloor).")
public record PlancherCouvertDto(
        @Schema(description = "Id de la ligne (CfgTrancheCoveredFloor.PKey).")
        UUID id,
        @Schema(description = "Catégorie d'article ou de sous-article de nature Bâtiment (StkItemCategory.id).")
        UUID categorieId,
        @Schema(description = "Code de la catégorie, lecture seule.")
        String categorieCode,
        @Schema(description = "Libellé de la catégorie, lecture seule.")
        String categorieDescription,
        @Schema(description = "Superficie couverte (m²).")
        BigDecimal surface
) {
}
