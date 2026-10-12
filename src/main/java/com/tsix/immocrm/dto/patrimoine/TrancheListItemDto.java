package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne d'une liste de tranches.")
public record TrancheListItemDto(
        UUID id,
        String code,
        String description,
        String ville,
        @Schema(description = "Date d'octroi des titres fonciers (CfgTranche.DeliveryDate, « Date octroi des T.F » d'OMRAN).")
        LocalDateTime dateOctroiTitresFonciers,
        @Schema(description = "Programme de la tranche (CfgProject.id).")
        UUID projetId,
        @Schema(description = "Code du programme de la tranche.")
        String projetCode
) {
}
