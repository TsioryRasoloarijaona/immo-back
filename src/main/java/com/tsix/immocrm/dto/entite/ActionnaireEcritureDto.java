package com.tsix.immocrm.dto.entite;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Création ou modification d'un actionnaire ; en PATCH, absent ou null = inchangé.")
public record ActionnaireEcritureDto(
        @Schema(description = "Code, unique (409). Obligatoire.") String code,
        @Schema(description = "Libellé. Obligatoire.") String description,
        @Schema(description = "Notes (500 caractères au plus).") String notes
) {
}
