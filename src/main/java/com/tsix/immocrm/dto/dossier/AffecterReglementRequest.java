package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Nouvelles lignes à imputer sur le reste à affecter d'un règlement existant.")
public record AffecterReglementRequest(

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<AffectationReglementRequest> affectations
) {
}
