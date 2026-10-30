package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Une action du journal (AdmWebHistorics), en lecture seule.")
public record LigneHistoriqueDto(
        @Schema(description = "Id de la ligne du journal (PKey).") UUID id,
        @Schema(description = "Id de l'objet concerné (ObjectId) ; peut être null.") UUID objetId,
        @Schema(description = "Date et heure de l'action (ActionDateTime).") LocalDateTime dateAction,
        @Schema(description = "Utilisateur (UserDescription), tel qu'enregistré au moment de l'action.") String utilisateur,
        @Schema(description = "Type technique de l'objet (ObjectType).") String entite,
        @Schema(description = "Libellé de l'objet (ObjectLabel).") String libelleEntite,
        @Schema(description = "Action brute : Added, Modified ou Deleted ; à traduire à l'écran.") String action) {
}
