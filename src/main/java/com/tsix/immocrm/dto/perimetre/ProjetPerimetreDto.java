package com.tsix.immocrm.dto.perimetre;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record ProjetPerimetreDto(

        UUID id,

        @Schema(example = "ARS")
        String code,

        @Schema(description = "Libellé du projet (CfgProject.Description).",
                example = "AYAD RESIDENCE BEL AIR")
        String libelle
) {
}
