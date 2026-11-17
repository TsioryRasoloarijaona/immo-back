package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ligne de la liste « Modèles de simulation lot ».")
public record ModeleSimulationListItemDto(
        @Schema(description = "PrjMarketPrevisionModel.PKey.") UUID id,
        @Schema(description = "Code du modèle.") String code,
        @Schema(description = "Description du modèle.") String description,
        @Schema(description = "Libellé de la catégorie de dépense ; null sans catégorie.") String categorie
) {
}
