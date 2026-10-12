package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Création (POST) ou modification partielle (PATCH) d'une zone de programme.")
public record ZoneProjetEcritureDto(
        @Schema(description = "Zone parente, du même programme. Création seulement : non modifiable ensuite.")
        UUID parentId,
        @Schema(description = "Code, unique parmi les zones sœurs du programme. Obligatoire à la création.")
        String code,
        @Schema(description = "Libellé. Obligatoire à la création.")
        String description,
        @Schema(description = "Notes (1000 caractères au plus).")
        String notes,
        @Schema(description = "Ordre parmi les zones sœurs ; défaut à la création : rang suivant.")
        Integer ordre
) {
}
