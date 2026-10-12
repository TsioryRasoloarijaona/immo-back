package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.AdresseComplete;
import com.tsix.immocrm.domain.ColonneTranche;
import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.entities.act.ActAccount;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheStanding;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.stk.StkTrancheHeading;
import com.tsix.immocrm.utils.Champs;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Lecture d'une tranche : fiche complète et ligne de liste (FEATURE-patrimoine-tranche-fiche.md §4.2, §4.4). */
final class VueTranche {

    private VueTranche() {
    }

    static TrancheDetailDto fiche(CfgTranche t) {
        CfgProject projet = t.getCfgProject();
        CfgCompany entite = Champs.de(projet, CfgProject::getCfgCompany);
        AdmCountry pays = t.getAdmCountry();
        TrancheDetailDto.TrancheDetailDtoBuilder b = TrancheDetailDto.builder()
                .id(t.getId())
                .projetId(Champs.de(projet, CfgProject::getId))
                .projetCode(Champs.de(projet, CfgProject::getCode))
                .projetLibelle(Champs.de(projet, CfgProject::getDescription))
                .entiteId(Champs.de(entite, CfgCompany::getId))
                .entiteCode(Champs.de(entite, CfgCompany::getCode))
                .entiteLibelle(Champs.de(entite, CfgCompany::getDescription))
                .paysId(Champs.de(pays, AdmCountry::getId))
                .paysLibelle(Champs.de(pays, AdmCountry::getDescription))
                .creePar(t.getUserCreate()).creeLe(t.getUserCreateDateTime())
                .modifiePar(t.getUserUpdate()).modifieLe(t.getUserUpdateDateTime());
        identite(b, t);
        dates(b, t);
        regles(b, t);
        references(b, t);
        foncier(b, t);
        return b.build();
    }

    private static void identite(TrancheDetailDto.TrancheDetailDtoBuilder b, CfgTranche t) {
        b.code(t.getCode()).description(t.getDescription()).descriptionLongue(t.getDescriptionLong())
                .adresse(t.getAddress()).adresse2(t.getAddress2()).adresse3(t.getAddress3())
                .codePostal(t.getZipCode()).ville(t.getCity())
                .telephone(t.getPhone()).mobile(t.getMobile()).fax(t.getFax()).email(t.getEmail())
                .siteWeb(t.getWebSite()).adresseSav(t.getSavAddress())
                .plancherCouvert(t.getCoveredFloorArea())
                .estEnVente(t.getIsForSale()).estEnLocation(t.getIsForRent())
                .visibleProjet(t.getIsForProject()).visibleAchat(t.getIsForBuy()).visibleSav(t.getIsForSav())
                .visiblePrm(t.getIsForPrm()).visibleRom(t.getIsForRom());
    }

    private static void dates(TrancheDetailDto.TrancheDetailDtoBuilder b, CfgTranche t) {
        b.dateDebutTravaux(t.getWorkStartDate()).dateFinTravaux(t.getWorkEndDate())
                .dateDebutCommercialisation(t.getMarketingStartDate())
                .dateFinCommercialisation(t.getMarketingEndDate())
                .dateOctroiTitresFonciers(t.getDeliveryDate())
                .datePermisHabiter(t.getOccupancyPermitsDate())
                .dateReception(t.getReceptionDate())
                .datePremiereRelance(t.getRevival1Date())
                .dateFinConcretisation(t.getEndDateConcretisation())
                .dateDebutFinalisation(t.getStartDateFinalisation());
    }

    private static void regles(TrancheDetailDto.TrancheDetailDtoBuilder b, CfgTranche t) {
        b.tauxPenaliteAnnulation(t.getCancellationPenalityPercent())
                .tauxPenaliteAnnulationRemise(t.getCancellationPenalityPercentDiscount())
                .penaliteTransfertArticle(t.getArticleTransferPenality())
                .penaliteTransfertArticleRemise(t.getArticleTransferPenalityDiscount())
                .penaliteTransfertTranche(t.getSliceTransferPenality())
                .penaliteTransfertTrancheRemise(t.getSliceTransferPenalityDiscount())
                .penaliteTransfertClient(t.getCustomerTransferPenality())
                .penaliteTransfertClientRemise(t.getCustomerTransferPenalityDiscount())
                .tauxPenalitePremierEngagement(t.getAgreementFirstPenaltyPercent())
                .tauxPenaliteDeuxiemeEngagement(t.getAgreementSecondPenaltyPercent())
                .delaiGarantie(t.getWarrantyDelay()).delaiGarantieSupplement(t.getWarrantyDelayAdded())
                .delaiPremierEngagement(t.getAgreementFirstDelay())
                .delaiDeuxiemeEngagement(t.getAgreementSecondDelay())
                .delaiSoldePaiement(t.getPaymentBalanceDelay())
                .delaiNotaireMinutes(t.getDelayLawyerMinute()).delaiToutSoldeMinutes(t.getDelayMinuteAllSettled())
                .delaiRetractation(t.getWithdrawalDelay()).delaiMinimumIntervention(t.getInterventionMinimumDelai())
                .delaiMaxEcheance(t.getMaxEcheanceDelai())
                .advancePercent(t.getAdvancePercent()).concretisationPercent(t.getConcretisationPercent())
                .maxVisitNumber(t.getMaxVisitNumber()).isWithVerifCredit(t.getIsWithVerifCredit())
                .creditPercent(t.getCreditPercent())
                .montantPreReservation(t.getBeforeReservationAmount())
                .montantPreReservationRemise(t.getBeforeReservationAmountDiscount())
                .avanceSurMontant(t.getAdvanceBasedOnAmount()).montantAvance(t.getAdvanceAmount())
                .advancePercentRemise(t.getAdvancePercentDiscount())
                .advancePercentRemiseActive(t.getIsActiveAdvancePercentDiscount())
                .fraisDossier(t.getDefaultExpenseAmount()).fraisDossierRemise(t.getDefaultExpenseAmountDiscount())
                .penaliteAnnulationSurPrixArticle(t.getCancellationPenalityBasedOnItemPrice())
                .autresFrais(t.getOtherExpenseAmount()).autresFraisRemise(t.getOtherExpenseAmountDiscount())
                .montantSyndic(t.getSyndicAmount()).montantSyndicRemise(t.getSyndicAmountDiscount())
                .periodeSyndic(t.getSyndicPeriod())
                .codeDroitRetractation(t.getCancellationRightCode()).tribunalCommerce(t.getCommerceTribunal())
                .verifierDelaiMaxEcheance(t.getIsWithVerifMaxEcheanceDelai())
                .ecartTypeConcretisation(t.getEcarttypeConcretisation())
                .ecartTypeFinalisation(t.getEcarttypeFinalisation())
                .cadeauParrain(t.getIsWithGiftForProposer()).cadeauAncienClient(t.getIsWithGiftForOldCustomer())
                .cadeauOrigineProspection(t.getIsWithGiftForProspectionOrigin())
                .cadeauxCumules(t.getAccumulatedGifts());
    }

    private static void references(TrancheDetailDto.TrancheDetailDtoBuilder b, CfgTranche t) {
        b.standingId(Champs.de(t.getCfgTrancheStanding(), CfgTrancheStanding::getId))
                .standingLibelle(Champs.de(t.getCfgTrancheStanding(), CfgTrancheStanding::getDescription))
                .niveauTerrainId(Champs.de(t.getLndLevel(), LndLevel::getId))
                .niveauTerrainLibelle(Champs.de(t.getLndLevel(), LndLevel::getDescription))
                .rubriqueId(Champs.de(t.getStkTrancheHeading(), StkTrancheHeading::getId))
                .rubriqueLibelle(Champs.de(t.getStkTrancheHeading(), StkTrancheHeading::getDescription))
                .responsableSavId(Champs.de(t.getCfgClaimsManager(), CfgTier::getId))
                .responsableSavLibelle(Champs.de(t.getCfgClaimsManager(), CfgTier::getDescription))
                .tiersAutruiId(Champs.de(t.getCfgTierOther(), CfgTier::getId))
                .tiersAutruiLibelle(Champs.de(t.getCfgTierOther(), CfgTier::getDescription))
                .projetFacturationId(Champs.de(t.getPrjProject(), PrjProject::getId))
                .projetFacturationLibelle(Champs.de(t.getPrjProject(), PrjProject::getDescription))
                .planComptableId(Champs.de(t.getActAccount(), ActAccount::getId))
                .planComptableLibelle(compte(t.getActAccount()));
    }

    private static void foncier(TrancheDetailDto.TrancheDetailDtoBuilder b, CfgTranche t) {
        b.dateMainLeveeAfh(t.getRaisedHandDateAFH()).dateCertificatDefinitif1(t.getTitleDate1())
                .dateMainLeveeBancaire(t.getRaisedHandDateBank()).dateCertificatDefinitif2(t.getTitleDate2())
                .dateDepotLotissementMunicipalite(t.getMunicipalityDateDeposit())
                .dateRetraitLotissementMunicipalite(t.getMunicipalityDateWithdrawal())
                .dateDepotLotissementNonApprouveOtc(t.getOTCDateNonApprovedDeposit())
                .dateReglementFraisOtc(t.getOTCDateSettlement())
                .dateDepotLotissementApprouveOtc(t.getOTCDateApprovedDeposit())
                .dateRetraitPlansMutationOtc(t.getOTCDateMutationWithdrawal())
                .dateAchevementPartages(t.getSharingDate()).dateDepotPartageCpf(t.getSharingDateDeposit())
                .datePaiementFraisPartageCpf(t.getCPFSharingSettlementDate())
                .dateRetraitPartageCpf(t.getCPFSharingWithdrawalDate())
                .dateRetraitCertificatsIndividuels(t.getTitleDate())
                .dateVerificationCertificats(t.getTitleDateVerification())
                .dateRepartitionCharges(t.getRepartitionDate());
    }

    private static String compte(ActAccount compte) {
        return compte == null ? null : compte.getCode() + " — " + compte.getDescription();
    }

    // ------------------------------------------------------------------ ligne de liste

    /** Les colonnes de {@link ColonneTranche#COUTEUSES} ne sont lues (proxys LAZY) que si demandées. */
    static TrancheListItemDto ligne(CfgTranche t, Set<ColonneTranche> colonnes) {
        TrancheDetailDto f = ficheSansReferences(t);
        TrancheListItemDto.TrancheListItemDtoBuilder b = depuisFiche(f);
        CfgProject projet = t.getCfgProject();
        b.projetId(Champs.de(projet, CfgProject::getId)).projetCode(Champs.de(projet, CfgProject::getCode));
        si(colonnes, ColonneTranche.PROJET_LIBELLE, () -> Champs.de(projet, CfgProject::getDescription),
                b::projetLibelle);
        si(colonnes, ColonneTranche.ENTITE_LIBELLE,
                () -> Champs.de(projet, CfgProject::getCfgCompany, CfgCompany::getDescription), b::entiteLibelle);
        si(colonnes, ColonneTranche.STANDING,
                () -> Champs.de(t.getCfgTrancheStanding(), CfgTrancheStanding::getDescription), b::standingLibelle);
        si(colonnes, ColonneTranche.NIVEAU_TERRAIN, () -> Champs.de(t.getLndLevel(), LndLevel::getDescription),
                b::niveauTerrainLibelle);
        si(colonnes, ColonneTranche.RESPONSABLE_SAV, () -> nom(t.getCfgClaimsManager()), b::responsableSavLibelle);
        si(colonnes, ColonneTranche.PAYS, () -> Champs.de(t.getAdmCountry(), AdmCountry::getDescription),
                b::paysLibelle);
        si(colonnes, ColonneTranche.ADRESSE_COMPLETE, () -> AdresseComplete.de(t.getCity(), t.getAddress(),
                t.getAddress2(), t.getAddress3(), t.getZipCode(),
                Champs.de(t.getAdmCountry(), AdmCountry::getDescription)), b::adresseComplete);
        si(colonnes, ColonneTranche.TIERS_AUTRUI, () -> nom(t.getCfgTierOther()), b::tiersAutruiLibelle);
        si(colonnes, ColonneTranche.PROJET_FACTURATION,
                () -> Champs.de(t.getPrjProject(), PrjProject::getDescription), b::projetFacturationLibelle);
        si(colonnes, ColonneTranche.PLAN_COMPTABLE, () -> compte(t.getActAccount()), b::planComptableLibelle);
        si(colonnes, ColonneTranche.RUBRIQUE,
                () -> Champs.de(t.getStkTrancheHeading(), StkTrancheHeading::getDescription), b::rubriqueLibelle);
        programme(b, projet, colonnes);
        return b.build();
    }

    /** Courrier et représentants : ceux du programme et de son entité, comme {@code CfgTrancheView}. */
    private static void programme(TrancheListItemDto.TrancheListItemDtoBuilder b, CfgProject projet,
                                  Set<ColonneTranche> colonnes) {
        si(colonnes, ColonneTranche.VILLE_SIGNATURE, () -> Champs.de(projet, CfgProject::getSignatureCity),
                b::villeSignature);
        si(colonnes, ColonneTranche.REPRESENTANT_LEGAL,
                () -> nom(Champs.de(projet, CfgProject::getCfgCompany, CfgCompany::getCfgTier)), b::representantLegal);
        si(colonnes, ColonneTranche.SIGNATAIRE, () -> nom(Champs.de(projet, CfgProject::getAdmSignatory)),
                b::signataireLibelle);
        si(colonnes, ColonneTranche.RESPONSABLE_TECHNIQUE,
                () -> nom(Champs.de(projet, CfgProject::getCfgTechnicalManager)), b::responsableTechniqueLibelle);
        si(colonnes, ColonneTranche.RESPONSABLE_COMMERCIAL,
                () -> nom(Champs.de(projet, CfgProject::getCfgSalesManager)), b::responsableCommercialLibelle);
    }

    /** Champs lus sur la seule ligne {@code CfgTranche}, sans traverser d'association. */
    private static TrancheDetailDto ficheSansReferences(CfgTranche t) {
        TrancheDetailDto.TrancheDetailDtoBuilder b = TrancheDetailDto.builder().id(t.getId())
                .creePar(t.getUserCreate()).creeLe(t.getUserCreateDateTime())
                .modifiePar(t.getUserUpdate()).modifieLe(t.getUserUpdateDateTime());
        identite(b, t);
        dates(b, t);
        regles(b, t);
        foncier(b, t);
        return b.build();
    }

    private static TrancheListItemDto.TrancheListItemDtoBuilder depuisFiche(TrancheDetailDto f) {
        return TrancheListItemDto.builder()
                .id(f.id()).code(f.code()).description(f.description()).descriptionLongue(f.descriptionLongue())
                .plancherCouvert(f.plancherCouvert())
                .estEnVente(f.estEnVente()).estEnLocation(f.estEnLocation()).visibleProjet(f.visibleProjet())
                .visibleAchat(f.visibleAchat()).visibleSav(f.visibleSav()).visiblePrm(f.visiblePrm())
                .visibleRom(f.visibleRom())
                .dateDebutTravaux(f.dateDebutTravaux()).dateFinTravaux(f.dateFinTravaux())
                .dateOctroiTitresFonciers(f.dateOctroiTitresFonciers()).datePermisHabiter(f.datePermisHabiter())
                .datePremiereRelance(f.datePremiereRelance())
                .dateDebutCommercialisation(f.dateDebutCommercialisation())
                .dateFinConcretisation(f.dateFinConcretisation()).dateDebutFinalisation(f.dateDebutFinalisation())
                .dateFinCommercialisation(f.dateFinCommercialisation()).dateReception(f.dateReception())
                .ville(f.ville()).telephone(f.telephone()).mobile(f.mobile()).fax(f.fax()).email(f.email())
                .siteWeb(f.siteWeb()).adresseSav(f.adresseSav())
                .montantPreReservation(f.montantPreReservation()).avanceSurMontant(f.avanceSurMontant())
                .advancePercent(f.advancePercent()).montantAvance(f.montantAvance()).fraisDossier(f.fraisDossier())
                .tauxPenaliteAnnulation(f.tauxPenaliteAnnulation())
                .penaliteAnnulationSurPrixArticle(f.penaliteAnnulationSurPrixArticle())
                .penaliteTransfertArticle(f.penaliteTransfertArticle())
                .penaliteTransfertTranche(f.penaliteTransfertTranche())
                .penaliteTransfertClient(f.penaliteTransfertClient()).autresFrais(f.autresFrais())
                .montantSyndic(f.montantSyndic()).periodeSyndic(f.periodeSyndic())
                .delaiPremierEngagement(f.delaiPremierEngagement()).delaiDeuxiemeEngagement(f.delaiDeuxiemeEngagement())
                .tauxPenalitePremierEngagement(f.tauxPenalitePremierEngagement())
                .tauxPenaliteDeuxiemeEngagement(f.tauxPenaliteDeuxiemeEngagement())
                .delaiSoldePaiement(f.delaiSoldePaiement()).delaiGarantie(f.delaiGarantie())
                .delaiGarantieSupplement(f.delaiGarantieSupplement())
                .codeDroitRetractation(f.codeDroitRetractation()).tribunalCommerce(f.tribunalCommerce())
                .delaiMinimumIntervention(f.delaiMinimumIntervention()).maxVisitNumber(f.maxVisitNumber())
                .delaiRetractation(f.delaiRetractation()).delaiMaxEcheance(f.delaiMaxEcheance())
                .verifierDelaiMaxEcheance(f.verifierDelaiMaxEcheance()).creditPercent(f.creditPercent())
                .isWithVerifCredit(f.isWithVerifCredit()).cadeauParrain(f.cadeauParrain())
                .cadeauAncienClient(f.cadeauAncienClient()).cadeauOrigineProspection(f.cadeauOrigineProspection())
                .cadeauxCumules(f.cadeauxCumules())
                .montantPreReservationRemise(f.montantPreReservationRemise())
                .advancePercentRemise(f.advancePercentRemise())
                .advancePercentRemiseActive(f.advancePercentRemiseActive())
                .fraisDossierRemise(f.fraisDossierRemise())
                .tauxPenaliteAnnulationRemise(f.tauxPenaliteAnnulationRemise())
                .penaliteTransfertArticleRemise(f.penaliteTransfertArticleRemise())
                .penaliteTransfertTrancheRemise(f.penaliteTransfertTrancheRemise())
                .penaliteTransfertClientRemise(f.penaliteTransfertClientRemise())
                .autresFraisRemise(f.autresFraisRemise()).montantSyndicRemise(f.montantSyndicRemise())
                .delaiNotaireMinutes(f.delaiNotaireMinutes()).delaiToutSoldeMinutes(f.delaiToutSoldeMinutes())
                .ecartTypeConcretisation(f.ecartTypeConcretisation()).ecartTypeFinalisation(f.ecartTypeFinalisation())
                .dateMainLeveeAfh(f.dateMainLeveeAfh()).dateCertificatDefinitif1(f.dateCertificatDefinitif1())
                .dateMainLeveeBancaire(f.dateMainLeveeBancaire()).dateCertificatDefinitif2(f.dateCertificatDefinitif2())
                .dateDepotLotissementMunicipalite(f.dateDepotLotissementMunicipalite())
                .dateRetraitLotissementMunicipalite(f.dateRetraitLotissementMunicipalite())
                .dateDepotLotissementNonApprouveOtc(f.dateDepotLotissementNonApprouveOtc())
                .dateReglementFraisOtc(f.dateReglementFraisOtc())
                .dateDepotLotissementApprouveOtc(f.dateDepotLotissementApprouveOtc())
                .dateRetraitPlansMutationOtc(f.dateRetraitPlansMutationOtc())
                .dateAchevementPartages(f.dateAchevementPartages()).dateDepotPartageCpf(f.dateDepotPartageCpf())
                .datePaiementFraisPartageCpf(f.datePaiementFraisPartageCpf())
                .dateRetraitPartageCpf(f.dateRetraitPartageCpf())
                .dateRetraitCertificatsIndividuels(f.dateRetraitCertificatsIndividuels())
                .dateVerificationCertificats(f.dateVerificationCertificats())
                .dateRepartitionCharges(f.dateRepartitionCharges())
                .creePar(f.creePar()).creeLe(f.creeLe()).modifiePar(f.modifiePar()).modifieLe(f.modifieLe());
    }

    private static <T> void si(Set<ColonneTranche> colonnes, ColonneTranche colonne, Supplier<T> valeur,
                               Consumer<T> setter) {
        if (colonnes.contains(colonne)) {
            setter.accept(valeur.get());
        }
    }

    private static String nom(CfgTier tiers) {
        return Champs.de(tiers, CfgTier::getDescription);
    }
}
