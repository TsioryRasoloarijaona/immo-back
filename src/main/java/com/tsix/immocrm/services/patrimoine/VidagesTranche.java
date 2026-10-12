package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgTranche;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Champs de {@code TrancheEcritureDto} que {@code champsAVider} accepte (FEATURE-patrimoine-tranche-fiche.md §1.1),
 * avec la valeur « vide » qu'OMRAN écrit : {@code ""} pour un texte, NULL pour une date ou une clé étrangère, 0 pour
 * un nombre. Les cases se décochent en envoyant {@code false}.
 */
final class VidagesTranche {

    static final ChampsAVider<CfgTranche> CATALOGUE = catalogue();

    private VidagesTranche() {
    }

    private static ChampsAVider<CfgTranche> catalogue() {
        ChampsAVider<CfgTranche> c = new ChampsAVider<>(Set.of("projetId", "code", "description"));
        textes(c);
        references(c);
        dates(c);
        datesFoncieres(c);
        montants(c);
        entiers(c);
        return c;
    }

    private static void textes(ChampsAVider<CfgTranche> c) {
        c.texte("descriptionLongue", CfgTranche::setDescriptionLong)
                .texte("adresse", CfgTranche::setAddress)
                .texte("adresse2", CfgTranche::setAddress2)
                .texte("adresse3", CfgTranche::setAddress3)
                .texte("codePostal", CfgTranche::setZipCode)
                .texte("ville", CfgTranche::setCity)
                .texte("telephone", CfgTranche::setPhone)
                .texte("mobile", CfgTranche::setMobile)
                .texte("fax", CfgTranche::setFax)
                .texte("email", CfgTranche::setEmail)
                .texte("siteWeb", CfgTranche::setWebSite)
                .texte("adresseSav", CfgTranche::setSavAddress)
                .texte("codeDroitRetractation", CfgTranche::setCancellationRightCode)
                .texte("tribunalCommerce", CfgTranche::setCommerceTribunal);
    }

    private static void references(ChampsAVider<CfgTranche> c) {
        c.nul("paysId", CfgTranche::setAdmCountry)
                .nul("standingId", CfgTranche::setCfgTrancheStanding)
                .nul("niveauTerrainId", CfgTranche::setLndLevel)
                .nul("rubriqueId", CfgTranche::setStkTrancheHeading)
                .nul("responsableSavId", CfgTranche::setCfgClaimsManager)
                .nul("tiersAutruiId", CfgTranche::setCfgTierOther)
                .nul("projetFacturationId", CfgTranche::setPrjProject)
                .nul("planComptableId", CfgTranche::setActAccount);
    }

    private static void dates(ChampsAVider<CfgTranche> c) {
        c.nul("dateDebutTravaux", CfgTranche::setWorkStartDate)
                .nul("dateFinTravaux", CfgTranche::setWorkEndDate)
                .nul("dateDebutCommercialisation", CfgTranche::setMarketingStartDate)
                .nul("dateFinCommercialisation", CfgTranche::setMarketingEndDate)
                .nul("dateOctroiTitresFonciers", CfgTranche::setDeliveryDate)
                .nul("datePermisHabiter", CfgTranche::setOccupancyPermitsDate)
                .nul("dateReception", CfgTranche::setReceptionDate)
                .nul("datePremiereRelance", CfgTranche::setRevival1Date)
                .nul("dateFinConcretisation", CfgTranche::setEndDateConcretisation)
                .nul("dateDebutFinalisation", CfgTranche::setStartDateFinalisation);
    }

    private static void datesFoncieres(ChampsAVider<CfgTranche> c) {
        c.nul("dateMainLeveeAfh", CfgTranche::setRaisedHandDateAFH)
                .nul("dateCertificatDefinitif1", CfgTranche::setTitleDate1)
                .nul("dateMainLeveeBancaire", CfgTranche::setRaisedHandDateBank)
                .nul("dateCertificatDefinitif2", CfgTranche::setTitleDate2)
                .nul("dateDepotLotissementMunicipalite", CfgTranche::setMunicipalityDateDeposit)
                .nul("dateRetraitLotissementMunicipalite", CfgTranche::setMunicipalityDateWithdrawal)
                .nul("dateDepotLotissementNonApprouveOtc", CfgTranche::setOTCDateNonApprovedDeposit)
                .nul("dateReglementFraisOtc", CfgTranche::setOTCDateSettlement)
                .nul("dateDepotLotissementApprouveOtc", CfgTranche::setOTCDateApprovedDeposit)
                .nul("dateRetraitPlansMutationOtc", CfgTranche::setOTCDateMutationWithdrawal)
                .nul("dateAchevementPartages", CfgTranche::setSharingDate)
                .nul("dateDepotPartageCpf", CfgTranche::setSharingDateDeposit)
                .nul("datePaiementFraisPartageCpf", CfgTranche::setCPFSharingSettlementDate)
                .nul("dateRetraitPartageCpf", CfgTranche::setCPFSharingWithdrawalDate)
                .nul("dateRetraitCertificatsIndividuels", CfgTranche::setTitleDate)
                .nul("dateVerificationCertificats", CfgTranche::setTitleDateVerification)
                .nul("dateRepartitionCharges", CfgTranche::setRepartitionDate);
    }

    private static void montants(ChampsAVider<CfgTranche> c) {
        BigDecimal zero = BigDecimal.ZERO;
        c.valeur("montantPreReservation", CfgTranche::setBeforeReservationAmount, zero)
                .valeur("montantPreReservationRemise", CfgTranche::setBeforeReservationAmountDiscount, zero)
                .valeur("montantAvance", CfgTranche::setAdvanceAmount, zero)
                .valeur("advancePercent", CfgTranche::setAdvancePercent, zero)
                .valeur("advancePercentRemise", CfgTranche::setAdvancePercentDiscount, zero)
                .valeur("fraisDossier", CfgTranche::setDefaultExpenseAmount, zero)
                .valeur("fraisDossierRemise", CfgTranche::setDefaultExpenseAmountDiscount, zero)
                .valeur("tauxPenaliteAnnulation", CfgTranche::setCancellationPenalityPercent, zero)
                .valeur("tauxPenaliteAnnulationRemise", CfgTranche::setCancellationPenalityPercentDiscount, zero)
                .valeur("penaliteTransfertArticle", CfgTranche::setArticleTransferPenality, zero)
                .valeur("penaliteTransfertArticleRemise", CfgTranche::setArticleTransferPenalityDiscount, zero)
                .valeur("penaliteTransfertTranche", CfgTranche::setSliceTransferPenality, zero)
                .valeur("penaliteTransfertTrancheRemise", CfgTranche::setSliceTransferPenalityDiscount, zero)
                .valeur("penaliteTransfertClient", CfgTranche::setCustomerTransferPenality, zero)
                .valeur("penaliteTransfertClientRemise", CfgTranche::setCustomerTransferPenalityDiscount, zero)
                .valeur("autresFrais", CfgTranche::setOtherExpenseAmount, zero)
                .valeur("autresFraisRemise", CfgTranche::setOtherExpenseAmountDiscount, zero)
                .valeur("montantSyndic", CfgTranche::setSyndicAmount, zero)
                .valeur("montantSyndicRemise", CfgTranche::setSyndicAmountDiscount, zero)
                .valeur("tauxPenalitePremierEngagement", CfgTranche::setAgreementFirstPenaltyPercent, zero)
                .valeur("tauxPenaliteDeuxiemeEngagement", CfgTranche::setAgreementSecondPenaltyPercent, zero)
                .valeur("creditPercent", CfgTranche::setCreditPercent, zero)
                .valeur("ecartTypeConcretisation", CfgTranche::setEcarttypeConcretisation, zero)
                .valeur("ecartTypeFinalisation", CfgTranche::setEcarttypeFinalisation, zero);
    }

    private static void entiers(ChampsAVider<CfgTranche> c) {
        Integer zero = 0;
        c.valeur("periodeSyndic", CfgTranche::setSyndicPeriod, zero)
                .valeur("delaiPremierEngagement", CfgTranche::setAgreementFirstDelay, zero)
                .valeur("delaiDeuxiemeEngagement", CfgTranche::setAgreementSecondDelay, zero)
                .valeur("delaiSoldePaiement", CfgTranche::setPaymentBalanceDelay, zero)
                .valeur("delaiGarantie", CfgTranche::setWarrantyDelay, zero)
                .valeur("delaiGarantieSupplement", CfgTranche::setWarrantyDelayAdded, zero)
                .valeur("delaiMinimumIntervention", CfgTranche::setInterventionMinimumDelai, zero)
                .valeur("maxVisitNumber", CfgTranche::setMaxVisitNumber, zero)
                .valeur("delaiRetractation", CfgTranche::setWithdrawalDelay, zero)
                .valeur("delaiMaxEcheance", CfgTranche::setMaxEcheanceDelai, zero)
                .valeur("delaiNotaireMinutes", CfgTranche::setDelayLawyerMinute, zero)
                .valeur("delaiToutSoldeMinutes", CfgTranche::setDelayMinuteAllSettled, zero);
    }
}
