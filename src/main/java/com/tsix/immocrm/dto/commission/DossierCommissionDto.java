package com.tsix.immocrm.dto.commission;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Dossier de vente de l'onglet Dossiers, avec ses commissions par type.")
public record DossierCommissionDto(
        UUID dossierId,
        @Schema(description = "N° du dossier.") String code,
        @Schema(description = "Statut du dossier ; null si sans statut.") String statut,
        String articleCode,
        @Schema(description = "Libellé de l'article.") String article,
        @Schema(description = "Catégorie de l'article.") String categorie,
        String client,
        UUID commercialId,
        String commercial,
        @Schema(description = "Le commercial est un prestataire (CfgTier.IsExtern).") boolean commercialExterne,
        UUID gestionnaireId,
        String gestionnaire,
        @Schema(description = "Le gestionnaire est un prestataire.") boolean gestionnaireExterne,
        UUID gestionnaire2Id,
        String gestionnaire2,
        @Schema(description = "Le gestionnaire 2 est un prestataire.") boolean gestionnaire2Externe,
        @Schema(description = "Code de la prospection du dossier.") String prospection,
        @Schema(description = "Date de concrétisation (AdvanceDate).") LocalDate dateConcretisation,
        @Schema(description = "Date minute / Enregistrement du contrat (AgreementSavingDate).")
        LocalDate dateEnregistrementContrat,
        @Schema(description = "Date envoi au notaire / avocat (AgreementDate).") LocalDate dateEnvoiNotaire,
        @Schema(description = "Date annulation / résiliation (CancelDate).") LocalDate dateAnnulation,
        @Schema(description = "Date du statut de finalisation 1 de la tranche ; null si non configurée.")
        LocalDate dateFinalisation1,
        @Schema(description = "Date du statut de finalisation 2 de la tranche ; null si non configurée.")
        LocalDate dateFinalisation2,
        @Schema(description = "Prix de vente TTC (PriceAfterDiscount).") BigDecimal prixVenteTtc,
        @Schema(description = "Σ des lignes Concrétisation.") BigDecimal commissionConcretisation,
        @Schema(description = "Σ des lignes Finalisation 1.") BigDecimal commissionFinalisation1,
        @Schema(description = "Σ des lignes Finalisation 2.") BigDecimal commissionFinalisation2,
        @Schema(description = "Σ des lignes Désistement (négative).") BigDecimal commissionDesistement,
        @Schema(description = "Part réglée des commissions du dossier.") BigDecimal montantRegle,
        @Schema(description = "Σ de toutes les commissions du dossier.") BigDecimal montantAPayer,
        @Schema(description = "montantAPayer − montantRegle.") BigDecimal montantReste) {
}
