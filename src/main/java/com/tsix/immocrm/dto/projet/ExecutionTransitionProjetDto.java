package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Exécution d'une transition de statut du projet.")
public record ExecutionTransitionProjetDto(
        @Schema(description = "Transition (id d'une TransitionProjetDto). Obligatoire.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID transitionId,
        @Schema(description = "Commentaire (StrHistory.Notes), obligatoire si la transition l'exige.") String commentaire,
        @Schema(description = "Date utilisateur, lue si la transition l'accepte ; défaut maintenant.") LocalDateTime dateUtilisateur
) {
}
