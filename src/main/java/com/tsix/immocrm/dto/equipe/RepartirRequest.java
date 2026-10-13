package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Pré-prospections à répartir automatiquement entre les commerciaux de leur projet.")
public record RepartirRequest(
        @Schema(description = "Pré-prospections à répartir (au moins une).")
        List<UUID> preProspectionIds,
        @Schema(description = "`true` (ou absent) : aperçu, rien n'est écrit ; `false` : les commerciaux sont posés.")
        Boolean simulation
) {
}
