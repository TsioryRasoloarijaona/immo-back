package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Noms repris de {@link TrancheDetailDto} quand le champ y existe déjà, anglais compris (FEATURE-patrimoine-ecriture.md §4.2). */
@Schema(description = "Création (POST) ou modification partielle (PATCH) d'une tranche (CfgTranche). "
        + "Taux en fraction : 0.2 = 20 %.")
public record TrancheEcritureDto(
        // -- Identité --
        @Schema(description = "Programme (CfgProject.id). Obligatoire à la création, non modifiable ensuite.")
        UUID projetId,
        @Schema(description = "Code saisi, unique sur toute la table (pas par programme). Obligatoire à la création.")
        String code,
        @Schema(description = "Libellé de la tranche. Obligatoire à la création.")
        String description,
        @Schema(description = "Description détaillée (500 caractères au plus).")
        String descriptionLongue,
        @Schema(description = "Visible dans le module Vente (IsForSale). Défaut true.")
        Boolean estEnVente,
        @Schema(description = "Visible dans le module Location (IsForRent). Défaut false.")
        Boolean estEnLocation,

        // -- Coordonnées --
        String adresse,
        String adresse2,
        String adresse3,
        String codePostal,
        @Schema(description = "Ville, texte libre.")
        String ville,
        @Schema(description = "Pays (AdmCountry.id).")
        UUID paysId,
        String telephone,
        String mobile,
        String fax,
        String email,
        String siteWeb,

        // -- Dates --
        LocalDateTime dateDebutTravaux,
        @Schema(description = "Fin des travaux, postérieure ou égale au lancement.")
        LocalDateTime dateFinTravaux,
        LocalDateTime dateDebutCommercialisation,
        @Schema(description = "Fin de commercialisation, postérieure ou égale au début.")
        LocalDateTime dateFinCommercialisation,
        @Schema(description = "Date d'octroi des titres fonciers (DeliveryDate).")
        LocalDateTime dateOctroiTitresFonciers,
        @Schema(description = "Date du permis d'habiter (OccupancyPermitsDate).")
        LocalDateTime datePermisHabiter,
        @Schema(description = "Date de réception (ReceptionDate).")
        LocalDateTime dateReception,
        @Schema(description = "Date de la première relance (Revival1Date).")
        LocalDateTime datePremiereRelance,
        @Schema(description = "Date de fin des concrétisations (EndDateConcretisation).")
        LocalDateTime dateFinConcretisation,
        @Schema(description = "Date de début des finalisations (StartDateFinalisation).")
        LocalDateTime dateDebutFinalisation,

        // -- Règles de gestion 1 --
        @Schema(description = "Montant de pré-réservation (BeforeReservationAmount), ≥ 0.")
        BigDecimal montantPreReservation,
        @Schema(description = "Montant de pré-réservation pour anciens clients, ≥ 0.")
        BigDecimal montantPreReservationRemise,
        @Schema(description = "Avance calculée sur un montant fixe plutôt qu'un pourcentage (AdvanceBasedOnAmount).")
        Boolean avanceSurMontant,
        @Schema(description = "Montant d'avance (AdvanceAmount), ≥ 0.")
        BigDecimal montantAvance,
        @Schema(description = "« Concrétisation » d'OMRAN, fraction ∈ [0 ; 1]. Recopiée dans concretisationPercent.")
        BigDecimal advancePercent,
        @Schema(description = "Concrétisation pour anciens clients, fraction ∈ [0 ; 1].")
        BigDecimal advancePercentRemise,
        @Schema(description = "Active la concrétisation anciens clients (IsActiveAdvancePercentDiscount).")
        Boolean advancePercentRemiseActive,
        @Schema(description = "Frais de dossier (DefaultExpenseAmount), ≥ 0.")
        BigDecimal fraisDossier,
        @Schema(description = "Frais de dossier anciens clients, ≥ 0.")
        BigDecimal fraisDossierRemise,
        @Schema(description = "Taux de pénalité d'annulation, fraction ∈ [0 ; 1].")
        BigDecimal tauxPenaliteAnnulation,
        @Schema(description = "Taux de pénalité d'annulation anciens clients, fraction ∈ [0 ; 1].")
        BigDecimal tauxPenaliteAnnulationRemise,
        @Schema(description = "Pénalité d'annulation calculée sur le prix de l'article. Défaut true.")
        Boolean penaliteAnnulationSurPrixArticle,
        @Schema(description = "Pénalité de transfert d'article, ≥ 0.")
        BigDecimal penaliteTransfertArticle,
        @Schema(description = "Pénalité de transfert d'article anciens clients, ≥ 0.")
        BigDecimal penaliteTransfertArticleRemise,
        @Schema(description = "Pénalité de transfert de tranche, ≥ 0.")
        BigDecimal penaliteTransfertTranche,
        @Schema(description = "Pénalité de transfert de tranche anciens clients, ≥ 0.")
        BigDecimal penaliteTransfertTrancheRemise,
        @Schema(description = "Pénalité de transfert de client, ≥ 0.")
        BigDecimal penaliteTransfertClient,
        @Schema(description = "Pénalité de transfert de client anciens clients, ≥ 0.")
        BigDecimal penaliteTransfertClientRemise,
        @Schema(description = "Autres frais (OtherExpenseAmount), ≥ 0.")
        BigDecimal autresFrais,
        @Schema(description = "Autres frais anciens clients, ≥ 0.")
        BigDecimal autresFraisRemise,
        @Schema(description = "Montant du syndic, ≥ 0.")
        BigDecimal montantSyndic,
        @Schema(description = "Montant du syndic anciens clients, ≥ 0.")
        BigDecimal montantSyndicRemise,
        @Schema(description = "Période du syndic (SyndicPeriod), entier ≥ 0.")
        Integer periodeSyndic,

        // -- Règles de gestion 2 --
        @Schema(description = "Délai du premier engagement, en jours. Défaut 60.")
        Integer delaiPremierEngagement,
        @Schema(description = "Délai du deuxième engagement, en jours. Défaut 30.")
        Integer delaiDeuxiemeEngagement,
        @Schema(description = "Taux de pénalité du premier engagement, fraction ∈ [0 ; 1].")
        BigDecimal tauxPenalitePremierEngagement,
        @Schema(description = "Taux de pénalité du deuxième engagement, fraction ∈ [0 ; 1].")
        BigDecimal tauxPenaliteDeuxiemeEngagement,
        @Schema(description = "Délai de solde du paiement (PaymentBalanceDelay), ≥ 0.")
        Integer delaiSoldePaiement,
        @Schema(description = "Délai de garantie (WarrantyDelay), ≥ 0.")
        Integer delaiGarantie,
        @Schema(description = "Délai de garantie supplémentaire (WarrantyDelayAdded), ≥ 0.")
        Integer delaiGarantieSupplement,
        @Schema(description = "Code du droit de rétractation (CancellationRightCode), texte.")
        String codeDroitRetractation,
        @Schema(description = "Tribunal de commerce (CommerceTribunal), texte.")
        String tribunalCommerce,
        @Schema(description = "Délai minimum d'intervention (InterventionMinimumDelai). Défaut 2.")
        Integer delaiMinimumIntervention,
        @Schema(description = "Nombre maximum de visites (MaxVisitNumber). Défaut 4.")
        Integer maxVisitNumber,
        @Schema(description = "Délai de rétractation en heures (WithdrawalDelay). Défaut 48.")
        Integer delaiRetractation,
        @Schema(description = "Délai maximum d'échéance (MaxEcheanceDelai), ≥ 0.")
        Integer delaiMaxEcheance,
        @Schema(description = "Contrôler le délai maximum d'échéance (IsWithVerifMaxEcheanceDelai).")
        Boolean verifierDelaiMaxEcheance,
        @Schema(description = "Crédit maximum, fraction ∈ [0 ; 1] (« le crédit max ne doit pas dépasser 100% »).")
        BigDecimal creditPercent,
        @Schema(description = "Contrôler le crédit maximum (IsWithVerifCredit).")
        Boolean isWithVerifCredit,

        // -- Autres --
        @Schema(description = "Délai notaire en minutes (DelayLawyerMinute). Défaut 60.")
        Integer delaiNotaireMinutes,
        @Schema(description = "Délai « tout soldé » en minutes (DelayMinuteAllSettled). Défaut 30.")
        Integer delaiToutSoldeMinutes,
        @Schema(description = "Écart-type des concrétisations (EcarttypeConcretisation), ≥ 0. Défaut 5.")
        BigDecimal ecartTypeConcretisation,
        @Schema(description = "Écart-type des finalisations (EcarttypeFinalisation), ≥ 0. Défaut 3.")
        BigDecimal ecartTypeFinalisation
) {
}
