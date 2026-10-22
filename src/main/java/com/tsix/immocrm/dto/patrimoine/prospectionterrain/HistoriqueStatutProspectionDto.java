package com.tsix.immocrm.dto.patrimoine.prospectionterrain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne d'historique de statut (StrHistory de la prospection), la plus ancienne d'abord.")
public record HistoriqueStatutProspectionDto(
        UUID id,
        @Schema(description = "DocDate : moment de l'écriture.") LocalDateTime date,
        @Schema(description = "« Création », ou « {transition}: {code}({login} & ) ».") String description,
        @Schema(description = "AdmUserDescription : nom affiché de l'auteur.") String utilisateur,
        @Schema(description = "StrWorkflowDescription, null pour la création.") String transition,
        @Schema(description = "Statut d'arrivée (StrStatusId).") UUID statutId,
        @Schema(description = "StrStatusDescription.") String statut,
        @Schema(description = "Notes : commentaire de la transition.") String commentaire,
        @Schema(description = "UserDate.") LocalDateTime dateUtilisateur
) {
}
