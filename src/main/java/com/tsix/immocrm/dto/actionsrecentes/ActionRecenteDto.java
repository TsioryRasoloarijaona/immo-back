package com.tsix.immocrm.dto.actionsrecentes;

import com.tsix.immocrm.domain.CibleActionRecente;
import com.tsix.immocrm.domain.TypeActionRecente;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActionRecenteDto(

        @Schema(description = "PKey de la ligne source (StrHistory ou ComActionMessage).")
        UUID id,

        TypeActionRecente type,

        LocalDateTime date,

        String libelle,

        @Schema(description = "AdmUser auteur de l'action ; null si inconnu (écriture sans utilisateur authentifié).")
        UUID auteurId,

        @Schema(description = "Nom affiché de l'auteur ; null si inconnu.")
        String auteurNom,

        CibleActionRecente cibleType,

        @Schema(description = "PKey de la prospection / pré-prospection (ComProspection) ou du dossier (ComFolder).")
        UUID cibleId,

        String cibleLibelle,

        @Schema(description = "Prospect (CfgTier) de la prospection, pour le lien secondaire ; null pour un dossier.")
        UUID prospectId
) {
}
