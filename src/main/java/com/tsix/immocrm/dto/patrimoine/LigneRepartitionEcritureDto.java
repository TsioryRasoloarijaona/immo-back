package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ligne de répartition saisie : un actionnaire (une seule fois) et son nombre de parts.")
public record LigneRepartitionEcritureDto(
        @Schema(description = "Actionnaire (CfgShareholder.PKey). Obligatoire.")
        UUID actionnaireId,
        @Schema(description = "Nombre de parts (actions), entier positif ou nul. Obligatoire.")
        Integer nombreParts
) {
}
