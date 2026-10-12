package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgTranche;

/**
 * Champs que « Charger à partir d'une autre tranche » recopie (OMRAN {@code CfgTranche.GetDefaultValuesFromTranche}) ;
 * jamais code, libellé, programme, dates, visibilités, délais de rétractation / d'échéance, crédit, écarts-types, tiers
 * autrui, projet de facturation, plan comptable, rubrique (FEATURE-patrimoine-tranche-onglets.md §3).
 */
final class CopieParametresTranche {

    private CopieParametresTranche() {
    }

    static void copier(CfgTranche s, CfgTranche c) {
        copierReferencesEtCoordonnees(s, c);
        copierReglesDeVente(s, c);
        copierRemises(s, c);
        copierDelaisEtCadeaux(s, c);
        // ConcretisationPercent est un accesseur d'OMRAN sur AdvancePercent, écrit tel quel en base.
        c.setConcretisationPercent(c.getAdvancePercent());
    }

    private static void copierReferencesEtCoordonnees(CfgTranche s, CfgTranche c) {
        c.setCfgTrancheStanding(s.getCfgTrancheStanding());
        c.setLndLevel(s.getLndLevel());
        c.setCfgClaimsManager(s.getCfgClaimsManager());
        c.setAddress(s.getAddress());
        c.setAddress2(s.getAddress2());
        c.setAddress3(s.getAddress3());
        c.setZipCode(s.getZipCode());
        c.setCity(s.getCity());
        c.setAdmCountry(s.getAdmCountry());
        c.setPhone(s.getPhone());
        c.setFax(s.getFax());
        c.setMobile(s.getMobile());
        c.setEmail(s.getEmail());
        c.setWebSite(s.getWebSite());
        c.setSavAddress(s.getSavAddress());
        c.setDescriptionLong(s.getDescriptionLong());
    }

    private static void copierReglesDeVente(CfgTranche s, CfgTranche c) {
        c.setBeforeReservationAmount(s.getBeforeReservationAmount());
        c.setAdvanceBasedOnAmount(s.getAdvanceBasedOnAmount());
        c.setAdvancePercent(s.getAdvancePercent());
        c.setAdvanceAmount(s.getAdvanceAmount());
        c.setDefaultExpenseAmount(s.getDefaultExpenseAmount());
        c.setCancellationPenalityPercent(s.getCancellationPenalityPercent());
        c.setCancellationPenalityBasedOnItemPrice(s.getCancellationPenalityBasedOnItemPrice());
        c.setArticleTransferPenality(s.getArticleTransferPenality());
        c.setSliceTransferPenality(s.getSliceTransferPenality());
        c.setCustomerTransferPenality(s.getCustomerTransferPenality());
        c.setSyndicAmount(s.getSyndicAmount());
        c.setSyndicPeriod(s.getSyndicPeriod());
        c.setOtherExpenseAmount(s.getOtherExpenseAmount());
        c.setAgreementFirstPenaltyPercent(s.getAgreementFirstPenaltyPercent());
        c.setAgreementSecondPenaltyPercent(s.getAgreementSecondPenaltyPercent());
        c.setCommerceTribunal(s.getCommerceTribunal());
        c.setCancellationRightCode(s.getCancellationRightCode());
    }

    private static void copierRemises(CfgTranche s, CfgTranche c) {
        c.setBeforeReservationAmountDiscount(s.getBeforeReservationAmountDiscount());
        c.setAdvancePercentDiscount(s.getAdvancePercentDiscount());
        c.setIsActiveAdvancePercentDiscount(s.getIsActiveAdvancePercentDiscount());
        c.setDefaultExpenseAmountDiscount(s.getDefaultExpenseAmountDiscount());
        c.setCancellationPenalityPercentDiscount(s.getCancellationPenalityPercentDiscount());
        c.setCustomerTransferPenalityDiscount(s.getCustomerTransferPenalityDiscount());
        c.setArticleTransferPenalityDiscount(s.getArticleTransferPenalityDiscount());
        c.setSliceTransferPenalityDiscount(s.getSliceTransferPenalityDiscount());
        c.setSyndicAmountDiscount(s.getSyndicAmountDiscount());
        c.setOtherExpenseAmountDiscount(s.getOtherExpenseAmountDiscount());
    }

    private static void copierDelaisEtCadeaux(CfgTranche s, CfgTranche c) {
        c.setAgreementFirstDelay(s.getAgreementFirstDelay());
        c.setAgreementSecondDelay(s.getAgreementSecondDelay());
        c.setPaymentBalanceDelay(s.getPaymentBalanceDelay());
        c.setWarrantyDelay(s.getWarrantyDelay());
        c.setWarrantyDelayAdded(s.getWarrantyDelayAdded());
        c.setInterventionMinimumDelai(s.getInterventionMinimumDelai());
        c.setMaxVisitNumber(s.getMaxVisitNumber());
        c.setDelayLawyerMinute(s.getDelayLawyerMinute());
        c.setDelayMinuteAllSettled(s.getDelayMinuteAllSettled());
        c.setIsWithGiftForProposer(s.getIsWithGiftForProposer());
        c.setIsWithGiftForOldCustomer(s.getIsWithGiftForOldCustomer());
        c.setIsWithGiftForProspectionOrigin(s.getIsWithGiftForProspectionOrigin());
        c.setAccumulatedGifts(s.getAccumulatedGifts());
    }
}
