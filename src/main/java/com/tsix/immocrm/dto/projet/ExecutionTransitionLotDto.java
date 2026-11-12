package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Exécution d'une transition de statut du lot.")
public record ExecutionTransitionLotDto(
        @Schema(description = "Transition (id d'une TransitionLotDto).", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID transitionId,
        @Schema(description = "Commentaire, écrit dans StrHistory.Notes ; obligatoire si la transition l'exige.") String notes
) {
}
