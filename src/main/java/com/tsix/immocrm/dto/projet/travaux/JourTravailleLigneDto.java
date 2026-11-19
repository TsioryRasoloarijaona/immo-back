package com.tsix.immocrm.dto.projet.travaux;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "Ligne de la liste Travaux chantier (table GrhWorkedDay et libellés joints).")
public record JourTravailleLigneDto(
        @Schema(description = "GrhWorkedDay.Pkey.") UUID id,
        StatutJourTravailleDto statut,
        @Schema(description = "Date (DocDate).") LocalDate date,
        @Schema(description = "Heure de début (TimeStart), null si vide.", type = "string", example = "08:00")
        @JsonFormat(pattern = "HH:mm") LocalTime heureDebut,
        @Schema(description = "Heure de fin (TimeEnd), null si vide.", type = "string", example = "17:30")
        @JsonFormat(pattern = "HH:mm") LocalTime heureFin,
        @Schema(description = "Personnel (CfgTier), null si vide.") String personnel,
        @Schema(description = "Niveau de nivellement (CfgTrancheLevelingDefinition.Code), null si vide.") String niveau,
        @Schema(description = "Type de travaux (GrhWorkedDayType.Description).") String type,
        @Schema(description = "Article (BuyItem.Description), null si vide.") String article,
        @Schema(description = "Groupe du type (GrhWorkedDayType.GroupDescription).") String groupe,
        @Schema(description = "Projet (PrjProject.Description), null pour un jour sans projet.") String projet,
        @Schema(description = "Code du niveau de nivellement, null si vide.") String niveauCode,
        @Schema(description = "Client (CfgTier), null si vide.") String client,
        @Schema(description = "Quantité (Quantity).") BigDecimal quantite,
        @Schema(description = "Unité (Unity).") String unite,
        @Schema(description = "Prix unitaire HT (UnitPriceHT).") BigDecimal prixUnitaireHT,
        @Schema(description = "Calculé au fil : prix unitaire HT × quantité, jamais stocké.") BigDecimal totalHT,
        @Schema(description = "Code (libre, non unique).") String code,
        @Schema(description = "Description.") String description,
        @Schema(description = "Notes.") String notes) {
}
