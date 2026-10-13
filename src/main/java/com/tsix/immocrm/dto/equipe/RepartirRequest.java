package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Pré-prospections à répartir automatiquement entre les commerciaux de leur projet : "
        + "`preProspectionIds` ou `filtre`, exactement un des deux.")
public record RepartirRequest(
        @Schema(description = "Pré-prospections à répartir (au moins une) ; absent ou vide quand `filtre` est donné.")
        List<UUID> preProspectionIds,
        @Schema(description = "Toutes les lignes que renverrait `GET /api/equipe/a-distribuer/lignes` avec ce filtre.")
        FiltreADistribuer filtre,
        @Schema(description = "`true` (ou absent) : aperçu, rien n'est écrit ; `false` : les commerciaux sont posés.")
        Boolean simulation
) {
}
