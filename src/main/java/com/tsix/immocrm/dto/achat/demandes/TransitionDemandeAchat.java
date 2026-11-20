package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Transition (StrWorkFlow) exécutable par l'appelant sur la demande ; même calcul que l'exécution.")
public record TransitionDemandeAchat(
        @Schema(description = "StrWorkFlow.PKey, à envoyer dans transitionId.") UUID id,
        @Schema(description = "Libellé de la transition.") String libelle,
        @Schema(description = "Statut d'arrivée.") StatutCibleDemandeAchat statutCible,
        @Schema(description = "Commentaire obligatoire (WithComment).") boolean avecCommentaire,
        @Schema(description = "Date utilisateur demandée (WithUserDate).") boolean avecDateUtilisateur,
        @Schema(description = "Identifiants d'un second utilisateur exigés (WithPassword).") boolean avecSecondValideur) {
}
