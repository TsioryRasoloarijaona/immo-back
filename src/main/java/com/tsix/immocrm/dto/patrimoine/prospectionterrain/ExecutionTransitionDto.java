package com.tsix.immocrm.dto.patrimoine.prospectionterrain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Exécution d'une transition de prospection terrain.")
public record ExecutionTransitionDto(
        @Schema(description = "Transition (id d'une TransitionProspectionTerrainDto). Obligatoire.") UUID transitionId,
        @Schema(description = "Commentaire (StrHistory.Notes), obligatoire si la transition l'exige.") String commentaire,
        @Schema(description = "Date utilisateur (ISO), lue si la transition l'accepte ; défaut maintenant.")
        LocalDateTime dateUtilisateur
) {
}
