package com.tsix.immocrm.dto.dossier.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Création d'une action sur un dossier ; la catégorie est déduite du statut, jamais fournie.")
public record CreerActionDossierRequest(
        @Schema(description = "Type d'action (ComActionMessageType.PKey), voir /api/referentiels/types-contact-prospection. Requis.")
        UUID typeId,
        @Schema(description = "Date de l'action ; absente = maintenant.")
        LocalDateTime date,
        @Schema(description = "Note de l'action. Requise, non vide.")
        String note,
        @Schema(description = "Date d'alerte éventuelle ; absente = pas d'alerte.")
        LocalDateTime dateAlerte
) {
}
