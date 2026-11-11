package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Critère d'avancement du projet (PrjProjectProgressCriteriaLine), par ordre.")
public record CritereAvancementDto(
        @Schema(description = "PrjProjectProgressCriteriaLine.PKey.") UUID id,
        @Schema(description = "O.D.T. (Sorting).") Integer ordre,
        @Schema(description = "Description, 255 caractères au plus.") String description,
        @Schema(description = "Catégorie de dépense (StlCategory.PKey).") UUID categorieId,
        @Schema(description = "Code de la catégorie, null si elle n'existe plus.") String categorieCode,
        @Schema(description = "Libellé de la catégorie.") String categorieDescription
) {
}
