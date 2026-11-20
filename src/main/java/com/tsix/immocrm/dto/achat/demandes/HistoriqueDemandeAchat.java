package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Ligne d'historique de statut de la demande (StrHistory), la plus récente d'abord.")
public record HistoriqueDemandeAchat(
        @Schema(description = "DocDate : moment de l'écriture.") LocalDateTime date,
        @Schema(description = "AdmUserDescription : nom affiché de l'auteur.") String utilisateur,
        @Schema(description = "« Création », ou « {transition}: {départ}=>{arrivée}({login} & {login du second}) ».") String description,
        @Schema(description = "Commentaire de la transition.") String notes,
        @Schema(description = "UserDate.") LocalDateTime dateUtilisateur) {
}
