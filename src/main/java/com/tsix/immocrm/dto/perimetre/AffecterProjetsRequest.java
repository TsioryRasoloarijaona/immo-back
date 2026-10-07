package com.tsix.immocrm.dto.perimetre;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record AffecterProjetsRequest(

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<UUID> projetIds
) {
}
