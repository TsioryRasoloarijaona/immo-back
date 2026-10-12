package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne voulue de l'onglet Plancher couvert.")
public record PlancherCouvertEcritureDto(
        @Schema(description = "Id d'une ligne existante de cette tranche ; absent = nouvelle ligne.")
        UUID id,
        @Schema(description = "Catégorie de nature Bâtiment, voir GET /api/referentiels/categories-bien. Obligatoire.")
        UUID categorieId,
        @Schema(description = "Superficie couverte en m² (≥ 0) ; absente = 0.")
        BigDecimal surface
) {
}
