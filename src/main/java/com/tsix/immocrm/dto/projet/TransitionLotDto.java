package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Transition de statut (StrWorkFlow) exécutable par l'appelant depuis le statut actuel du lot.")
public record TransitionLotDto(
        @Schema(description = "StrWorkFlow.PKey, à envoyer dans transitionId.") UUID id,
        @Schema(description = "Libellé de la transition.") String libelle,
        @Schema(description = "Statut d'arrivée (StrStatus.PKey).") UUID statutArriveeId,
        @Schema(description = "Libellé du statut d'arrivée.") String statutArriveeLibelle,
        @Schema(description = "Nom de couleur .NET du statut d'arrivée, null si absent.", example = "LimeGreen")
        String statutArriveeCouleur
) {
}
