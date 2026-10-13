package com.tsix.immocrm.dto.dossier.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Action d'un dossier de vente (ComActionMessage rattachée par ComFolderId).")
public record ActionDossierDto(
        @Schema(description = "Identifiant de l'action (ComActionMessage.PKey).")
        UUID id,
        @Schema(description = "Date de l'action (ComActionMessage.DocDate).")
        LocalDateTime date,
        @Schema(description = "Type d'action (ComActionMessageType.PKey) : Note, Téléphone sortant, Instruction…")
        UUID typeId,
        @Schema(description = "Libellé du type d'action.")
        String type,
        @Schema(description = "Libellé de la catégorie, déduite du statut du dossier à la création.")
        String categorie,
        @Schema(description = "Note de l'action (ComActionMessage.AlertMessage).")
        String note,
        @Schema(description = "Auteur : libellé du compte qui a saisi (UserName, sinon UserCreate).")
        String auteur,
        @Schema(description = "Date d'alerte éventuelle (ComActionMessage.AlertValidityDate), null sans alerte.")
        LocalDateTime dateAlerte,
        @Schema(description = "Vrai pour une instruction (type « Instruction »).")
        boolean instruction,
        @Schema(description = "Commercial destinataire d'une instruction (ComActionMessage.CfgEmployeeId).")
        UUID destinataireId,
        @Schema(description = "Nom du commercial destinataire d'une instruction.")
        String destinataireNom,
        @Schema(description = "Vrai si l'instruction n'a pas encore été lue par son destinataire (IsNotRead).")
        boolean nonLue
) {
}
