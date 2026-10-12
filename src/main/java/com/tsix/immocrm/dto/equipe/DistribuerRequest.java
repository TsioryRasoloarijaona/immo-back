package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Distribution en lot : chaque pré-prospection est transformée comme par "
        + "`POST /api/prospections/{id}/transformer`, dans une seule transaction.")
public record DistribuerRequest(

        @Schema(description = "Pré-prospections à distribuer (au moins une).", requiredMode = Schema.RequiredMode.REQUIRED)
        List<UUID> preProspectionIds,

        @Schema(description = "Employé (CfgTier) du commercial destinataire — `commercialId` de `GET /api/equipe/commerciaux`.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID commercialId,

        @Schema(description = "Tranche posée sur chaque ligne ; facultative si toutes en portent déjà une.")
        UUID trancheId
) {
}
