package com.tsix.immocrm.dto.projet.travaux;

import com.tsix.immocrm.services.workflow.SecondValideur;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Corps de l'exécution d'une transition du jour travaillé ; statut de départ relu en base.")
public record ExecutionTransitionJourTravailleDto(
        @Schema(description = "id d'une TransitionJourTravailleDto du GET.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID transitionId,
        @Schema(description = "Commentaire (StrHistory.Notes) ; obligatoire quand avecCommentaire.") String notes,
        @Schema(description = "StrHistory.UserDate quand avecDateUtilisateur ; absente = l'instant de l'exécution.")
        LocalDateTime dateUtilisateur,
        @Schema(description = "Identifiants du second utilisateur quand avecSecondValideur.") SecondValideur secondValideur) {
}
