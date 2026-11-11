package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne d'historique de statut du projet (StrHistory), la plus récente d'abord.")
public record HistoriqueProjetDto(
        @Schema(description = "StrHistory.PKey.") UUID id,
        @Schema(description = "DocDate : moment de l'écriture.") LocalDateTime date,
        @Schema(description = "« Création », ou « {transition}: {départ}=>{arrivée}({login} & ) ».") String description,
        @Schema(description = "AdmUserDescription : nom affiché de l'auteur.") String utilisateur,
        @Schema(description = "StrWorkflowDescription, null pour la création.") String transition,
        @Schema(description = "StrStatusDescription : statut d'arrivée.") String statut,
        @Schema(description = "Notes : commentaire de la transition.") String commentaire,
        @Schema(description = "UserDate.") LocalDateTime dateUtilisateur
) {
}
