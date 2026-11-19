package com.tsix.immocrm.dto.projet.travaux;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "Saisie d'un jour travaillé ; un champ obligatoire absent donne un 400 qui le nomme.")
public record JourTravailleEcritureDto(
        @Schema(description = "Société (CfgCompany.PKey). Obligatoire ; la changer vide le personnel.") UUID societeId,
        @Schema(description = "Projet (PrjProject.PKey) ; null = jour sans projet. Non habilité → 404.") UUID projetId,
        @Schema(description = "Type de travaux (GrhWorkedDayType.PKey). Obligatoire.") UUID typeId,
        @Schema(description = "Date. Obligatoire.") LocalDate date,
        @Schema(description = "Heure de début, null si vide.", type = "string", example = "08:00")
        @JsonFormat(pattern = "HH:mm") LocalTime heureDebut,
        @Schema(description = "Heure de fin, null si vide ; non contrôlée contre l'heure de début.", type = "string",
                example = "17:30")
        @JsonFormat(pattern = "HH:mm") LocalTime heureFin,
        @Schema(description = "Personnel (CfgTier.PKey).") UUID employeId,
        @Schema(description = "Client (CfgTier.PKey).") UUID clientId,
        @Schema(description = "Niveau de nivellement (CfgTrancheLevelingDefinition.PKey).") UUID niveauId,
        @Schema(description = "Zone (CfgProjectZone.PKey). Obligatoire.") UUID zoneId,
        @Schema(description = "Article (BuyItem.PKey).") UUID articleId,
        @Schema(description = "Quantité. Obligatoire.") BigDecimal quantite,
        @Schema(description = "Quantité 1. Obligatoire.") BigDecimal quantite1,
        @Schema(description = "Unité ; absente, celle du type.") String unite,
        @Schema(description = "Prix unitaire HT ; absent, celui du type.") BigDecimal prixUnitaireHT,
        @Schema(description = "Taux de TVA (fraction : 0.2 = 20 %) ; absent, celui du type.") BigDecimal tva,
        @Schema(description = "Unité 1 ; absente, celle du type.") String unite1,
        @Schema(description = "Prix unitaire HT 1 ; absent, celui du type.") BigDecimal prixUnitaireHT1,
        @Schema(description = "Taux de TVA 1 (fraction) ; absent, celui du type.") BigDecimal tva1,
        @Schema(description = "Code libre, non unique.") String code,
        @Schema(description = "Description.") String description,
        @Schema(description = "Notes (1 000 caractères au plus).") String notes) {
}
