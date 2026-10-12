package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.TrancheEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.ChampInvalideException;

import java.math.BigDecimal;

import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.LONGUEUR_COURTE;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.LONGUEUR_MOYENNE;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.fraction;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.positifOuNul;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.si;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.texte;

/**
 * Valeurs initiales d'une tranche et report des champs saisis (FEATURE-patrimoine-ecriture.md §4). Les valeurs
 * initiales sont celles d'OMRAN ({@code CfgTranche.cs} l.147–223), sauf {@code IsForSale} : vrai chez nous.
 */
final class SaisieTranche {

    private static final BigDecimal CINQ = BigDecimal.valueOf(5);
    private static final BigDecimal TROIS = BigDecimal.valueOf(3);

    private SaisieTranche() {
    }

    /** Aucune colonne numérique, booléenne ou texte qu'OMRAN écrit ne reste NULL (§2.4). */
    static void initialiserDefauts(CfgTranche t) {
        initialiserTextes(t);
        initialiserDrapeaux(t);
        initialiserDelais(t);
        initialiserMontants(t);
        initialiserMontantsMorts(t);
    }

    private static void initialiserTextes(CfgTranche t) {
        t.setDescriptionLong("");
        t.setAddress("");
        t.setAddress2("");
        t.setAddress3("");
        t.setZipCode("");
        t.setCity("");
        t.setPhone("");
        t.setMobile("");
        t.setFax("");
        t.setEmail("");
        t.setWebSite("");
        t.setCommerceTribunal("");
        t.setCancellationRightCode("");
        t.setSavAddress("");
    }

    private static void initialiserDrapeaux(CfgTranche t) {
        t.setIsForSale(true);
        t.setIsForRent(false);
        t.setIsForProject(false);
        t.setIsForBuy(false);
        t.setIsForSav(false);
        t.setIsForPrm(false);
        t.setIsForRom(false);
        t.setIsWithGiftForProposer(false);
        t.setIsWithGiftForOldCustomer(false);
        t.setIsWithGiftForProspectionOrigin(false);
        t.setAccumulatedGifts(false);
        t.setIsWithVerifCredit(false);
        t.setIsWithVerifMaxEcheanceDelai(false);
        t.setAdvanceBasedOnAmount(false);
        t.setIsActiveAdvancePercentDiscount(false);
        t.setCancellationPenalityBasedOnItemPrice(true);
    }

    private static void initialiserDelais(CfgTranche t) {
        t.setAgreementFirstDelay(60);
        t.setAgreementSecondDelay(30);
        t.setWithdrawalDelay(48);
        t.setInterventionMinimumDelai(2);
        t.setMaxVisitNumber(4);
        t.setDelayLawyerMinute(60);
        t.setDelayMinuteAllSettled(30);
        t.setSyndicPeriod(0);
        t.setWarrantyDelay(0);
        t.setWarrantyDelayAdded(0);
        t.setPaymentBalanceDelay(0);
        t.setMaxEcheanceDelai(0);
        t.setCountItemBasePrevision(0);
    }

    private static void initialiserMontants(CfgTranche t) {
        t.setEcarttypeConcretisation(CINQ);
        t.setEcarttypeFinalisation(TROIS);
        t.setAdvancePercent(BigDecimal.ZERO);
        t.setConcretisationPercent(BigDecimal.ZERO);
        t.setAdvancePercentDiscount(BigDecimal.ZERO);
        t.setAdvanceAmount(BigDecimal.ZERO);
        t.setBeforeReservationAmount(BigDecimal.ZERO);
        t.setBeforeReservationAmountDiscount(BigDecimal.ZERO);
        t.setDefaultExpenseAmount(BigDecimal.ZERO);
        t.setDefaultExpenseAmountDiscount(BigDecimal.ZERO);
        t.setCancellationPenalityPercent(BigDecimal.ZERO);
        t.setCancellationPenalityPercentDiscount(BigDecimal.ZERO);
        t.setArticleTransferPenality(BigDecimal.ZERO);
        t.setArticleTransferPenalityDiscount(BigDecimal.ZERO);
        t.setSliceTransferPenality(BigDecimal.ZERO);
        t.setSliceTransferPenalityDiscount(BigDecimal.ZERO);
        t.setCustomerTransferPenality(BigDecimal.ZERO);
        t.setCustomerTransferPenalityDiscount(BigDecimal.ZERO);
        t.setOtherExpenseAmount(BigDecimal.ZERO);
        t.setOtherExpenseAmountDiscount(BigDecimal.ZERO);
        t.setSyndicAmount(BigDecimal.ZERO);
        t.setSyndicAmountDiscount(BigDecimal.ZERO);
        t.setAgreementFirstPenaltyPercent(BigDecimal.ZERO);
        t.setAgreementSecondPenaltyPercent(BigDecimal.ZERO);
        t.setCreditPercent(BigDecimal.ZERO);
    }

    // Colonnes non exposées (prévisions, unités, plancher couvert) : 0 comme OMRAN, jamais NULL.
    private static void initialiserMontantsMorts(CfgTranche t) {
        t.setCoveredFloorArea(BigDecimal.ZERO);
        t.setTotalSaleBasePrevision(BigDecimal.ZERO);
        t.setBaseEcarttypeConcretisation(BigDecimal.ZERO);
        t.setBaseEcarttypeFinalisation(BigDecimal.ZERO);
        t.setBaseConcretisationPercent(BigDecimal.ZERO);
        t.setUnitConcretisation(BigDecimal.ZERO);
        t.setUnitFinalisation(BigDecimal.ZERO);
        t.setUnitTurnover(BigDecimal.ZERO);
        t.setUnitTurnoverHT(BigDecimal.ZERO);
    }

    /** Champs facultatifs : absent ou {@code null} = inchangé (PATCH), chaîne vide = vider. */
    static void appliquer(CfgTranche t, TrancheEcritureDto d) {
        appliquerCoordonnees(t, d);
        appliquerDates(t, d);
        appliquerReglesGestion1(t, d);
        appliquerPenalites(t, d);
        appliquerReglesGestion2(t, d);
        appliquerAutres(t, d);
        // ConcretisationPercent est un accesseur d'OMRAN sur AdvancePercent, écrit tel quel en base.
        t.setConcretisationPercent(t.getAdvancePercent());
    }

    private static void appliquerCoordonnees(CfgTranche t, TrancheEcritureDto d) {
        texte(d.descriptionLongue(), LONGUEUR_MOYENNE, "descriptionLongue", t::setDescriptionLong);
        si(d.estEnVente(), t::setIsForSale);
        si(d.estEnLocation(), t::setIsForRent);
        texte(d.adresse(), LONGUEUR_COURTE, "adresse", t::setAddress);
        texte(d.adresse2(), LONGUEUR_COURTE, "adresse2", t::setAddress2);
        texte(d.adresse3(), LONGUEUR_COURTE, "adresse3", t::setAddress3);
        texte(d.codePostal(), LONGUEUR_COURTE, "codePostal", t::setZipCode);
        texte(d.ville(), LONGUEUR_COURTE, "ville", t::setCity);
        texte(d.telephone(), LONGUEUR_COURTE, "telephone", t::setPhone);
        texte(d.mobile(), LONGUEUR_COURTE, "mobile", t::setMobile);
        texte(d.fax(), LONGUEUR_COURTE, "fax", t::setFax);
        texte(d.email(), LONGUEUR_COURTE, "email", t::setEmail);
        texte(d.siteWeb(), LONGUEUR_COURTE, "siteWeb", t::setWebSite);
    }

    private static void appliquerDates(CfgTranche t, TrancheEcritureDto d) {
        si(d.dateDebutTravaux(), t::setWorkStartDate);
        si(d.dateFinTravaux(), t::setWorkEndDate);
        si(d.dateDebutCommercialisation(), t::setMarketingStartDate);
        si(d.dateFinCommercialisation(), t::setMarketingEndDate);
        si(d.dateOctroiTitresFonciers(), t::setDeliveryDate);
        si(d.datePermisHabiter(), t::setOccupancyPermitsDate);
        si(d.dateReception(), t::setReceptionDate);
        si(d.datePremiereRelance(), t::setRevival1Date);
        si(d.dateFinConcretisation(), t::setEndDateConcretisation);
        si(d.dateDebutFinalisation(), t::setStartDateFinalisation);
        ReglesSaisie.periode(t.getWorkStartDate(), t.getWorkEndDate(), "dateFinTravaux", "des travaux");
        ReglesSaisie.periode(t.getMarketingStartDate(), t.getMarketingEndDate(),
                "dateFinCommercialisation", "de commercialisation");
    }

    private static void appliquerReglesGestion1(CfgTranche t, TrancheEcritureDto d) {
        si(positifOuNul(d.montantPreReservation(), "montantPreReservation"), t::setBeforeReservationAmount);
        si(positifOuNul(d.montantPreReservationRemise(), "montantPreReservationRemise"),
                t::setBeforeReservationAmountDiscount);
        si(d.avanceSurMontant(), t::setAdvanceBasedOnAmount);
        si(positifOuNul(d.montantAvance(), "montantAvance"), t::setAdvanceAmount);
        si(fraction(d.advancePercent(), "advancePercent"), t::setAdvancePercent);
        si(fraction(d.advancePercentRemise(), "advancePercentRemise"), t::setAdvancePercentDiscount);
        si(d.advancePercentRemiseActive(), t::setIsActiveAdvancePercentDiscount);
        si(positifOuNul(d.fraisDossier(), "fraisDossier"), t::setDefaultExpenseAmount);
        si(positifOuNul(d.fraisDossierRemise(), "fraisDossierRemise"), t::setDefaultExpenseAmountDiscount);
        si(positifOuNul(d.autresFrais(), "autresFrais"), t::setOtherExpenseAmount);
        si(positifOuNul(d.autresFraisRemise(), "autresFraisRemise"), t::setOtherExpenseAmountDiscount);
        si(positifOuNul(d.montantSyndic(), "montantSyndic"), t::setSyndicAmount);
        si(positifOuNul(d.montantSyndicRemise(), "montantSyndicRemise"), t::setSyndicAmountDiscount);
        si(positifOuNul(d.periodeSyndic(), "periodeSyndic"), t::setSyndicPeriod);
    }

    private static void appliquerPenalites(CfgTranche t, TrancheEcritureDto d) {
        si(fraction(d.tauxPenaliteAnnulation(), "tauxPenaliteAnnulation"), t::setCancellationPenalityPercent);
        si(fraction(d.tauxPenaliteAnnulationRemise(), "tauxPenaliteAnnulationRemise"),
                t::setCancellationPenalityPercentDiscount);
        si(d.penaliteAnnulationSurPrixArticle(), t::setCancellationPenalityBasedOnItemPrice);
        si(positifOuNul(d.penaliteTransfertArticle(), "penaliteTransfertArticle"), t::setArticleTransferPenality);
        si(positifOuNul(d.penaliteTransfertArticleRemise(), "penaliteTransfertArticleRemise"),
                t::setArticleTransferPenalityDiscount);
        si(positifOuNul(d.penaliteTransfertTranche(), "penaliteTransfertTranche"), t::setSliceTransferPenality);
        si(positifOuNul(d.penaliteTransfertTrancheRemise(), "penaliteTransfertTrancheRemise"),
                t::setSliceTransferPenalityDiscount);
        si(positifOuNul(d.penaliteTransfertClient(), "penaliteTransfertClient"), t::setCustomerTransferPenality);
        si(positifOuNul(d.penaliteTransfertClientRemise(), "penaliteTransfertClientRemise"),
                t::setCustomerTransferPenalityDiscount);
        si(fraction(d.tauxPenalitePremierEngagement(), "tauxPenalitePremierEngagement"),
                t::setAgreementFirstPenaltyPercent);
        si(fraction(d.tauxPenaliteDeuxiemeEngagement(), "tauxPenaliteDeuxiemeEngagement"),
                t::setAgreementSecondPenaltyPercent);
    }

    private static void appliquerReglesGestion2(CfgTranche t, TrancheEcritureDto d) {
        si(positifOuNul(d.delaiPremierEngagement(), "delaiPremierEngagement"), t::setAgreementFirstDelay);
        si(positifOuNul(d.delaiDeuxiemeEngagement(), "delaiDeuxiemeEngagement"), t::setAgreementSecondDelay);
        si(positifOuNul(d.delaiSoldePaiement(), "delaiSoldePaiement"), t::setPaymentBalanceDelay);
        si(positifOuNul(d.delaiGarantie(), "delaiGarantie"), t::setWarrantyDelay);
        si(positifOuNul(d.delaiGarantieSupplement(), "delaiGarantieSupplement"), t::setWarrantyDelayAdded);
        texte(d.codeDroitRetractation(), LONGUEUR_COURTE, "codeDroitRetractation", t::setCancellationRightCode);
        texte(d.tribunalCommerce(), LONGUEUR_COURTE, "tribunalCommerce", t::setCommerceTribunal);
        si(positifOuNul(d.delaiMinimumIntervention(), "delaiMinimumIntervention"), t::setInterventionMinimumDelai);
        si(positifOuNul(d.maxVisitNumber(), "maxVisitNumber"), t::setMaxVisitNumber);
        si(positifOuNul(d.delaiRetractation(), "delaiRetractation"), t::setWithdrawalDelay);
        si(positifOuNul(d.delaiMaxEcheance(), "delaiMaxEcheance"), t::setMaxEcheanceDelai);
        si(d.verifierDelaiMaxEcheance(), t::setIsWithVerifMaxEcheanceDelai);
        if (d.creditPercent() != null && d.creditPercent().compareTo(BigDecimal.ONE) > 0) {
            // Message de CfgTranche.Verif d'OMRAN.
            throw new ChampInvalideException("creditPercent", "Le crédit max ne doit pas dépasser 100% !");
        }
        si(fraction(d.creditPercent(), "creditPercent"), t::setCreditPercent);
        si(d.isWithVerifCredit(), t::setIsWithVerifCredit);
    }

    private static void appliquerAutres(CfgTranche t, TrancheEcritureDto d) {
        si(positifOuNul(d.delaiNotaireMinutes(), "delaiNotaireMinutes"), t::setDelayLawyerMinute);
        si(positifOuNul(d.delaiToutSoldeMinutes(), "delaiToutSoldeMinutes"), t::setDelayMinuteAllSettled);
        si(positifOuNul(d.ecartTypeConcretisation(), "ecartTypeConcretisation"), t::setEcarttypeConcretisation);
        si(positifOuNul(d.ecartTypeFinalisation(), "ecartTypeFinalisation"), t::setEcarttypeFinalisation);
    }
}
