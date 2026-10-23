package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Transition à exécuter sur l'étude (pas de note : OMRAN n'en demande pas).")
public record DemandeTransitionEtudeDto(
        @Schema(description = "Transition (GET …/transitions), obligatoire.") UUID transitionId) {
}
