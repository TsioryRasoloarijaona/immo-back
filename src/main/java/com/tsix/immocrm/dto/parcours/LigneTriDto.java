package com.tsix.immocrm.dto.parcours;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Pré-prospection de la Boîte de tri, avec les champs du formulaire et ses doublons.")
public record LigneTriDto(
        UUID prospectionId,
        @Schema(example = "26/06025")
        String code,
        @Schema(description = "Nom de la fiche prospect, sinon nom saisi au premier contact.")
        String nom,
        @Schema(description = "Fiche prospect liée (CfgTier) ; null tant qu'elle n'est pas créée — requise pour qualifier.")
        UUID prospectId,
        @Schema(example = "0661248735")
        String telephone,
        @Schema(description = "Indicatif pays du mobile de la fiche ; null si le numéro ne vient pas de la fiche.", example = "212")
        String indicatif,
        String email,
        @Schema(description = "Date de réception (ComProspection.DocDate).")
        LocalDateTime recuLe,
        @Schema(description = "Jours écoulés depuis la réception, au jour de Casablanca.", example = "2")
        long ageJours,
        @Schema(description = "Message du formulaire (ComProspection.Notes).")
        String message,
        @Schema(description = "Origine (ComProspectionOrigin.Code).", example = "FACEBOOK")
        String source,
        @Schema(description = "Support de l'origine (ComProspectionOriginSupport.Description).")
        String sourceSupport,
        @Schema(description = "Campagne : action commerciale rattachée (ComAction.Description).")
        String campagne,
        UUID trancheId,
        String trancheCode,
        @Schema(example = "TRIBECA")
        String projetCode,
        @Schema(description = "Étape (ComProspectionKind.Code) ; null sans étape.")
        String etape,
        @Schema(description = "Échéance de la relance ouverte (après « Pas joignable » : demain 10:00) ; null sinon.")
        LocalDateTime prochaineRelance,
        @Schema(description = "Doublons visibles de l'appelant ; vide si aucun.")
        List<DoublonTriDto> doublons
) {
}
