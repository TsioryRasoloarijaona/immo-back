package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Enregistrer la fiche d'un lot : champ absent = inchangé, null explicite = vidé ; taux en pourcentage 0–100.")
public record ModifierLotRequest(
        @Schema(description = "Désignation, non vide, 255 caractères au plus.") String description,
        @Schema(description = "Fournisseur (CfgTier.PKey) ; null = aucun ; inconnu → 400.") UUID fournisseurId,
        @Schema(description = "Devise (StlCurrency.PKey) ; null = aucune ; inconnue → 400.") UUID deviseId,
        @Schema(description = "Catégorie (StlCategory.PKey) ; obligatoire si présent ; inconnue → 400.") UUID categorieId,
        @Schema(description = "Type de lot (PrjMarketType.PKey) ; obligatoire si présent ; inconnu → 400.") UUID typeId,
        @Schema(description = "Durée de garantie en mois (WarrantyInMonths).") BigDecimal dureeGarantieMois,
        @Schema(description = "Taux d'avance en pourcentage (AdvanceRatio).", example = "10") BigDecimal tauxAvancePct,
        @Schema(description = "Taux de retenue de garantie en pourcentage (WarrantyRatio).") BigDecimal tauxRetenueGarantiePct,
        @Schema(description = "Taux de compte prorata en pourcentage (ProportionRatio).") BigDecimal tauxCompteProrataPct,
        @Schema(description = "Taux d'assurance décennale en pourcentage (InsuranceDECRatio).") BigDecimal tauxAssuranceDECPct,
        @Schema(description = "Montant d'assurance décennale (InsuranceDECAmount).") BigDecimal montantAssuranceDEC,
        @Schema(description = "Taux d'assurance tous risques chantier en pourcentage (InsuranceTRCRatio).")
        BigDecimal tauxAssuranceTRCPct,
        @Schema(description = "Montant d'assurance tous risques chantier (InsuranceTRCAmount).") BigDecimal montantAssuranceTRC,
        @Schema(description = "TVA par défaut en pourcentage (DefaultVat).", example = "20") BigDecimal tvaDefautPct,
        @Schema(description = "Délai de paiement en jours (PaymentDelay).") Integer delaiPaiementJours,
        @Schema(description = "Estimation confidentielle HT (EstimatedPriceHTManual) ; droit d'estimation manuelle.")
        BigDecimal montantForfaitaireHT,
        @Schema(description = "Estimation confidentielle TTC (EstimatedPriceTTCManual) ; droit d'estimation manuelle.")
        BigDecimal montantForfaitaireTTC,
        @Schema(description = "Est. de base : date de début (StartDateManual) ; droit d'estimation manuelle.") LocalDate dateDebutBase,
        @Schema(description = "Est. de base : durée en jours (DelayManual) ; droit d'estimation manuelle.") Integer delaiBaseJours,
        @Schema(description = "Est. de base : date de fin (EndDateManual) ; droit d'estimation manuelle.") LocalDate dateFinBase,
        @Schema(description = "Lancement de l'appel d'offres (LaunchOfTenderDate).") LocalDate dateLancementAppelOffres,
        @Schema(description = "Retrait du dossier de consultation (WithdrawalTenderDocumentsDate).") LocalDate dateRetraitDossiers,
        @Schema(description = "Échéance de remise des offres (TenderMaturityDate).") LocalDate dateEcheanceRemiseOffres,
        @Schema(description = "Report de l'appel d'offres (TendersResortDate).") LocalDate dateReportAppelOffres,
        @Schema(description = "Ouverture des plis (BidOpeningDate).") LocalDate dateOuverturePlis,
        @Schema(description = "Rapport de dépouillement (TendersReportDate).") LocalDate dateRapportDepouillement,
        @Schema(description = "Adjudication (TenderDate).") LocalDate dateAdjudication,
        @Schema(description = "Signature du marché (ContractSignatureDate).") LocalDate dateSignatureMarche,
        @Schema(description = "Ordre de service (ServiceOrderDate).") LocalDate dateOrdreService,
        @Schema(description = "Lancement des travaux (WorkLaunchDate).") LocalDate dateLancementTravaux,
        @Schema(description = "Achèvement des travaux (WorkCompletionDate).") LocalDate dateAchevementTravaux,
        @Schema(description = "Recollement (ReattachmentDate).") LocalDate dateRecollement,
        @Schema(description = "Variation dans la masse des travaux en pourcentage (MassChangeRatio).") BigDecimal variationMassePct,
        @Schema(description = "Remboursement de l'approvisionnement en pourcentage (StockPileRatio).")
        BigDecimal tauxRemboursementApproPct,
        @Schema(description = "Total des points (TotalPoints).") Integer totalPoints,
        @Schema(description = "Type de paiement (StlSettlementType.PKey) ; null = aucun ; inconnu → 400.") UUID typePaiementId,
        @Schema(description = "Remise sur travaux supplémentaires (ApplyDiscountOnAdditionalWork).") Boolean remiseSurTravauxSupp,
        @Schema(description = "Retenue sur travaux supplémentaires (ApplyWithholdingTaxOnAdditionalWork).")
        Boolean retenueSurTravauxSupp,
        @Schema(description = "Saisie sur les quantités exécutées (ExecutedQuantityBasedInsert).") Boolean saisieSurQuantites,
        @Schema(description = "Avancement par ligne de décompte (CanEditWorkingOutLineByAchivement).") Boolean avancementParLigne,
        @Schema(description = "Bloquer la restitution de la retenue de garantie (BlokWarrantyRestitution).")
        Boolean restitutionGarantieBloquee,
        @Schema(description = "Date de résiliation (CancellationDate).") LocalDate dateResiliation,
        @Schema(description = "Cause de la résiliation (CancellationCause), 500 caractères au plus.") String causeResiliation,
        @Schema(description = "Description détaillée (DescriptionLong), 500 caractères au plus.") String descriptionDetaillee,
        @Schema(description = "Autres notes (Notes), 1000 caractères au plus.") String notes
) {
}
