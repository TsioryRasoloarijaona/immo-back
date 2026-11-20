package com.tsix.immocrm.dto.projet.travaux;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Transition (StrWorkFlow) que l'appelant peut exécuter sur le jour travaillé ; même calcul que le POST.")
public record TransitionJourTravailleDto(
        @Schema(description = "StrWorkFlow.PKey : valeur de transitionId à l'exécution.") UUID id,
        @Schema(description = "StrWorkFlow.Description.") String libelle,
        @Schema(description = "Statut auquel mène la transition.") StatutCibleJourTravailleDto statutCible,
        @Schema(description = "WithComment : notes obligatoires à l'exécution.") boolean avecCommentaire,
        @Schema(description = "WithUserDate : dateUtilisateur proposée à la saisie.") boolean avecDateUtilisateur,
        @Schema(description = "WithPassword : login et mot de passe d'un second utilisateur exigés.") boolean avecSecondValideur) {

    @Schema(description = "Statut d'arrivée de la transition (StrStatus du jour travaillé).")
    public record StatutCibleJourTravailleDto(
            @Schema(description = "StrStatus.PKey du statut d'arrivée.") UUID id,
            @Schema(description = "StrStatus.Code du statut d'arrivée.") String code,
            @Schema(description = "StrStatus.Description du statut d'arrivée.") String libelle,
            @Schema(description = "StrStatus.ColorName (nom de couleur .NET) ; null si absent.") String couleur) {
    }
}
