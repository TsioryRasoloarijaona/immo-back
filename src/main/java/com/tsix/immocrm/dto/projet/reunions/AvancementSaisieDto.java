package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Saisie d'une cellule de l'onglet Avancements ; la ligne est créée si absente.")
public record AvancementSaisieDto(
        @Schema(description = "Critère du projet (PrjProjectProgressCriteriaLine.PKey).",
                requiredMode = Schema.RequiredMode.REQUIRED) UUID critereId,
        @Schema(description = "Niveau du projet (CfgTrancheLevelingDefinition.PKey).",
                requiredMode = Schema.RequiredMode.REQUIRED) UUID niveauId,
        @Schema(description = "Pourcentage de 0 à 100 ; 0 ou absent = cellule vidée.", example = "40") BigDecimal pourcentage) {
}
