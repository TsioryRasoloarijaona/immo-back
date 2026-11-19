package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "En-tête saisi d'une demande interne d'achat (création et modification). Code, statut, entité : jamais lus.")
public record SaisieDemandeAchat(
        @Schema(description = "Procédure d'achat (BuySelectionKind.PKey), obligatoire.") UUID procedureId,
        @Schema(description = "Société (CfgCompany.PKey) habilitée, obligatoire.") UUID societeId,
        @Schema(description = "Description, obligatoire (255 caractères).") String description,
        @Schema(description = "Date de la demande ; défaut aujourd'hui.") LocalDate date,
        @Schema(description = "Projet habilité (PrjProject.PKey) ; null = sans projet.") UUID projetId,
        @Schema(description = "Type de lot (PrjMarketType.PKey).") UUID typeLotId,
        @Schema(description = "Catégorie (StlCategory.PKey).") UUID categorieId,
        @Schema(description = "Devise (StlCurrency.PKey).") UUID deviseId,
        @Schema(description = "Module (AdmModule.PKey) ; défaut module Achat (BUY).") UUID moduleId,
        @Schema(description = "Déclencheur (CfgTier.PKey) ; défaut l'employé du compte connecté.") UUID declencheurId,
        @Schema(description = "Description longue (500 caractères).") String descriptionLongue,
        @Schema(description = "TVA par défaut.") BigDecimal tvaDefaut,
        @Schema(description = "Prix estimé HT.") BigDecimal prixEstimeHT,
        @Schema(description = "Prix estimé TTC ; contrôlé par la procédure (R6).") BigDecimal prixEstimeTTC,
        @Schema(description = "Mode de paiement (StlSettlementType.PKey).") UUID paiementId,
        @Schema(description = "Délai de paiement en jours.") Integer delaiPaiement,
        @Schema(description = "Dates d'appel d'offre ; absent = toutes vides.") DatesDemandeAchat dates,
        @Schema(description = "Notes (1000 caractères).") String notes,
        @Schema(description = "Texte de la lettre de sélection.") String texteLettre) {
}
