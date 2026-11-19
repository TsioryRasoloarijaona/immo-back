package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Ligne de la liste des demandes internes d'achat (BuySelection).")
public record DemandeAchatLigne(
        @Schema(description = "BuySelection.PKey.") UUID id,
        @Schema(description = "Procédure d'achat (BuySelectionKind.Description).") String procedure,
        @Schema(description = "Statut stocké.") StatutDemandeAchat statut,
        @Schema(description = "Code yy/nnnnn, généré à la création.") String code,
        @Schema(description = "Date de la demande (DocDate).") LocalDate date,
        @Schema(description = "Description.") String description,
        @Schema(description = "Société (CfgCompany.Description).") String entite,
        @Schema(description = "Projet (PrjProject.Description) ; null sans projet.") String projet,
        @Schema(description = "Lot (PrjMarket.Description) ; null sans lot.") String lot,
        @Schema(description = "Module (AdmModule.Description).") String module,
        @Schema(description = "Fournisseur retenu (raison sociale) ; null tant qu'aucune offre n'est sélectionnée.") String fournisseur,
        @Schema(description = "Catégorie (StlCategory.Description).") String categorie,
        @Schema(description = "Déclencheur (raison sociale de l'employé).") String declencheur,
        @Schema(description = "Notes.") String notes,
        @Schema(description = "Prix estimé TTC (PriceEstimatedTTC).") BigDecimal montantEstimeTTC,
        @Schema(description = "Dernière demande de son groupe (IsTheLast) ; false = lecture seule.") boolean estDerniere) {
}
