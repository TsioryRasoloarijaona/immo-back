package com.tsix.immocrm.dto.parcours;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Relance ouverte de « Ma journée » : l'interaction (ComActionMessage) qui porte la prochaine action.")
public record RelanceJourneeDto(
        @Schema(description = "Interaction qui porte la relance (ComActionMessage.PKey).")
        UUID interactionId,
        UUID prospectionId,
        @Schema(description = "Vrai si la ligne est encore une pré-prospection (elle se traite dans la Boîte de tri).")
        boolean preProspection,
        @Schema(description = "Nom de la fiche prospect, sinon nom saisi au premier contact.")
        String nom,
        @Schema(example = "0661248735")
        String telephone,
        @Schema(description = "Indicatif pays du mobile de la fiche ; null si le numéro ne vient pas de la fiche.", example = "212")
        String indicatif,
        @Schema(example = "TRIBECA")
        String projetCode,
        String trancheCode,
        @Schema(description = "Étape de la prospection (ComProspectionKind.Code) ; null sans étape.", example = "3 QUALIFIÉ")
        String etape,
        @Schema(description = "Échéance de la relance (ComActionMessage.AlertValidityDate).")
        LocalDateTime echeance,
        @Schema(description = "Jours entre l'échéance et aujourd'hui ; 0 pour une relance du jour.", example = "3")
        long joursDeRetard,
        @Schema(description = "Message de la relance (ComActionMessage.AlertMessage).")
        String message,
        @Schema(description = "Résultat du dernier contact, qui dit l'intention (visite, rappel…).", example = "Rendez vous pris")
        String resultat
) {
}
