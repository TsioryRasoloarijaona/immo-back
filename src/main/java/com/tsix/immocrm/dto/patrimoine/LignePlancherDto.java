package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Ligne de l'onglet Plancher couvert (CfgTrancheCoveredFloor).")
public record LignePlancherDto(
        @Schema(description = "PKey de la ligne ; absent au PUT = nouvelle ligne.") UUID id,
        @Schema(description = "Catégorie d'article (StkItemCategory) de nature Bâtiment, obligatoire.") UUID categorieId,
        @Schema(description = "Superficie (Area), m², ≥ 0 ; absente = 0.") BigDecimal surface,
        @Schema(description = "Code de la catégorie.", accessMode = READ_ONLY) String categorieCode,
        @Schema(description = "Description de la catégorie.", accessMode = READ_ONLY) String categorieDescription
) implements LignesTrancheDto.Ligne {
}
