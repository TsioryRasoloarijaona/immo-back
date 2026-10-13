package com.tsix.immocrm.dto.parcours;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Nouveau lead de « Ma journée » : pré-prospection du commercial, ouverte, non traitée (0 action, étape de départ).")
public record LeadJourneeDto(
        @Schema(description = "Identifiant de la pré-prospection (ComProspection.PKey).")
        UUID prospectionId,
        @Schema(example = "26/06025")
        String code,
        @Schema(description = "Nom de la fiche prospect, sinon nom saisi au premier contact (TempDescription).")
        String nom,
        @Schema(description = "Mobile ou fixe de la fiche, sinon numéro du premier contact (TempTel).", example = "0661248735")
        String telephone,
        @Schema(description = "Indicatif pays du mobile de la fiche ; null si le numéro ne vient pas de la fiche.", example = "212")
        String indicatif,
        @Schema(description = "Code du projet de la tranche visée ; null sans tranche.", example = "TRIBECA")
        String projetCode,
        @Schema(description = "Code de la tranche visée ; null sans tranche.")
        String trancheCode,
        @Schema(description = "Date de réception du lead (ComProspection.DocDate).")
        LocalDateTime recuLe,
        @Schema(description = "Jours écoulés depuis la réception, calculés au jour de Casablanca.", example = "2")
        long ageJours,
        @Schema(description = "Message du formulaire (ComProspection.Notes).")
        String message,
        @Schema(description = "Origine du lead (ComProspectionOrigin.Code).", example = "FACEBOOK")
        String source
) {
}
