package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.services.workflow.SecondValideur;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Exécution d'une transition de statut du lot.")
public record ExecutionTransitionLotDto(
        @Schema(description = "Transition (id d'une TransitionLotDto).", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID transitionId,
        @Schema(description = "Commentaire ; obligatoire si avecCommentaire, conservé seulement avec commentaire ou date.") String notes,
        @Schema(description = "Date utilisateur (avecDateUtilisateur) ; absente = maintenant.") LocalDateTime dateUtilisateur,
        @Schema(description = "Second utilisateur, obligatoire si avecSecondValideur.") SecondValideur secondValideur
) {
}
