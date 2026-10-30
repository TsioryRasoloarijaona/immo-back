package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne du journal des e-mails.")
public record LigneEmailDto(
        @Schema(description = "Identifiant de l'e-mail (AdmEMailHistory.PKey).") UUID id,
        @Schema(description = "Date d'envoi, ISO locale.", example = "2026-03-10T09:15:30") LocalDateTime date,
        @Schema(description = "Utilisateur à l'origine de l'envoi.") String utilisateur,
        @Schema(description = "Adresse de l'expéditeur.") String expediteur,
        @Schema(description = "Destinataires, tels qu'enregistrés.") String destinataires,
        @Schema(description = "Objet de l'e-mail.") String objet,
        @Schema(description = "Envoi automatique ; null si OMRAN ne l'a pas renseigné.") Boolean automatique,
        @Schema(description = "Vrai si envoyé, faux si en échec ; null si OMRAN ne l'a pas renseigné.") Boolean envoye,
        @Schema(description = "Message d'erreur ; null s'il est vide.") String erreur,
        @Schema(description = "Libellé de l'objet métier concerné (dossier, tiers…).") String libelleObjet) {
}
