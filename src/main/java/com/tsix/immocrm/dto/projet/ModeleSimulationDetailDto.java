package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche d'un modèle de simulation lot.")
public record ModeleSimulationDetailDto(
        @Schema(description = "PrjMarketPrevisionModel.PKey.") UUID id,
        @Schema(description = "Code du modèle.") String code,
        @Schema(description = "Description du modèle.") String description,
        @Schema(description = "Catégorie de dépense (StlCategory.PKey) ; null sans catégorie.") UUID categorieId,
        @Schema(description = "Libellé de la catégorie de dépense ; null sans catégorie.") String categorie,
        @Schema(description = "Lignes triées par temps croissant.") List<LigneModeleDto> lignes
) {
}
