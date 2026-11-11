package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Import des critères d'avancement d'un autre projet.")
public record ImportCriteresDto(
        @Schema(description = "Projet source (PrjProject.PKey), autre que le projet importateur. Obligatoire.",
                requiredMode = Schema.RequiredMode.REQUIRED) UUID projetSourceId
) {
}
