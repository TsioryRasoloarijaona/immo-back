package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record TrancheListItemDto(
        UUID id,
        String code,
        String description,
        String ville,
        @Schema(description = "CfgTranche.DeliveryDate : date d'octroi des titres fonciers (ex-dateLivraison).")
        LocalDateTime dateOctroiTitresFonciers,
        @Schema(description = "Programme (CfgProject) de la tranche.") UUID projetId,
        String projetCode
) {
}
