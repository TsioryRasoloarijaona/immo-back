package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Fiche d'un projet de travaux : champs de la liste, textes, responsable et dates (toutes nullables).")
public record ProjetChantierDetailDto(
        @Schema(description = "Identifiant du projet (PrjProject.PKey).") UUID id,
        @Schema(description = "Code du projet.", example = "ARS-PRJ") String code,
        @Schema(description = "Désignation du projet.") String description,
        @Schema(description = "Code de la société (CfgCompany) ; null si absente.") String societeCode,
        @Schema(description = "Libellé de la société.") String societeDescription,
        @Schema(description = "Code du statut du projet (StrStatus).") String statutCode,
        @Schema(description = "Libellé du statut du projet.") String statutDescription,
        @Schema(description = "Nombre de lots du projet.") long nbLots,
        @Schema(description = "Σ des montants marché HT des lots.") BigDecimal totalMarchesHT,
        @Schema(description = "Σ des montants marché TTC des lots.") BigDecimal totalMarchesTTC,
        @Schema(description = "Nombre de décomptes de tous les lots.") long nbDecomptes,
        @Schema(description = "Σ des montants TTC stockés des décomptes.") BigDecimal totalDecomptesTTC,
        @Schema(description = "Σ des nets à payer des factures de décompte.") BigDecimal totalFacture,
        @Schema(description = "Σ des règlements non annulés de ces factures.") BigDecimal totalRegle,
        @Schema(description = "totalFacture − totalRegle.") BigDecimal totalResteARegler,
        @Schema(description = "Vrai si aucun lot et code « AAAA-… » (centre de coûts annuel).") boolean estCentreDeCouts,
        @Schema(description = "Description longue ; null si vide.") String descriptionLongue,
        @Schema(description = "Notes ; null si vides.") String notes,
        @Schema(description = "Adresse du chantier ; null si vide.") String adresse,
        @Schema(description = "Responsable du projet (CfgTierManagerId → CfgTier) ; null si non renseigné.")
        UUID responsableId,
        @Schema(description = "Nom du responsable (CfgTier.Description).") String responsableNom,
        @Schema(description = "Date de début (StartDate).") LocalDate dateDebut,
        @Schema(description = "Délai en jours (Delay).") Integer delaiJours,
        @Schema(description = "Date de fin (EndDate).") LocalDate dateFin,
        @Schema(description = "Début des travaux prévu (ProvisionalWorkStartDate).") LocalDate dateDebutTravauxPrevue,
        @Schema(description = "Fin des travaux prévue (ProvisionalCompletingWorkDate).") LocalDate dateFinTravauxPrevue,
        @Schema(description = "Début réel des travaux (RealWorkStartDate).") LocalDate dateDebutTravauxReelle,
        @Schema(description = "Fin réelle des travaux (RealCompletingWorkDate).") LocalDate dateFinTravauxReelle,
        @Schema(description = "Date de création (UserCreateDateTime).") LocalDateTime dateCreation,
        @Schema(description = "Auteur de la création (UserCreate).") String creePar) {
}
