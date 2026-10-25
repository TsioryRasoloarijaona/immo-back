package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.ColonneTranche;
import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.dto.patrimoine.TrancheEcritureDto;
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
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.COURT;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.fraction;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.motif;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ordre;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;

@Service
@Transactional(readOnly = true)
public class TrancheService {

    private static final String INTROUVABLE = "Tranche introuvable: ";
    private static final Set<String> OBLIGATOIRES = Set.of("code", "description", "projetId");

    private final CfgTrancheRepository trancheRepository;
    private final TrancheVisibleRepository visibles;
    private final CfgProjectRepository projectRepository;
    private final AdmCountryRepository countryRepository;
    private final ReferencesPatrimoine references;
    private final TracePatrimoine trace;

    public TrancheService(CfgTrancheRepository trancheRepository, TrancheVisibleRepository visibles,
                          CfgProjectRepository projectRepository, AdmCountryRepository countryRepository,
                          ReferencesPatrimoine references, TracePatrimoine trace) {
        this.trancheRepository = trancheRepository;
        this.visibles = visibles;
        this.projectRepository = projectRepository;
        this.countryRepository = countryRepository;
        this.references = references;
        this.trace = trace;
    }

    /** {@code colonnes} : sélection résolue du catalogue ; les libellés de référence non demandés ne sont pas lus. */
    public Page<TrancheListItemDto> findAll(UUID projetId, String search, Set<ColonneTranche> colonnes,
                                            Pageable pageable) {
        return trancheRepository.rechercher(projetId, motif(search), pageable).map(t -> toListItem(t, colonnes));
    }

    public TrancheDetailDto findById(UUID id) {
        return toDetail(visible(id));
    }

    /** Tranche d'une entité active (ou sans entité), sinon 404 : seule recherche par id des routes de la tranche (R4). */
    CfgTranche visible(UUID id) {
        return visibles.trancheVisible(id).orElseThrow(() -> new ResourceNotFoundException(INTROUVABLE + id));
    }

    @Transactional
    public TrancheDetailDto creerTranche(TrancheEcritureDto saisie) {
        if (saisie.projetId() == null) {
            throw new InvalidRequestException("Le programme est obligatoire.");
        }
        CfgProject projet = projectRepository.findVisible(saisie.projetId())
                .orElseThrow(() -> new InvalidRequestException("projetId : programme inconnu (" + saisie.projetId() + ")."));
        CfgTranche tranche = nouvelleTranche();
        tranche.setCfgProject(projet);
        appliquer(tranche, saisie);
        TracePatrimoine.Trace t = trace.maintenant();
        tranche.setUserCreate(t.auteur());
        tranche.setUserCreateDateTime(t.le());
        tranche.setUserUpdate(t.auteur());
        tranche.setUserUpdateDateTime(t.le());
        trancheRepository.save(tranche);
        trace.habiliterCreateur(null, tranche);
        return toDetail(tranche);
    }

    @Transactional
    public TrancheDetailDto modifierTranche(UUID id, TrancheEcritureDto saisie) {
        CfgTranche tranche = visible(id);
        if (saisie.projetId() != null && !saisie.projetId().equals(Champs.de(tranche.getCfgProject(), CfgProject::getId))) {
            throw new InvalidRequestException("Le programme d'une tranche ne se modifie pas.");
        }
        appliquer(tranche, saisie);
        TracePatrimoine.Trace t = trace.maintenant();
        tranche.setUserUpdate(t.auteur());
        tranche.setUserUpdateDateTime(t.le());
        return toDetail(trancheRepository.save(tranche));
    }

    /** Défauts OMRAN ({@code CfgTranche.cs} l.147–223) sauf IsForSale vrai : FEATURE-patrimoine-ecriture.md §4.4. */
    private static CfgTranche nouvelleTranche() {
        CfgTranche t = new CfgTranche();
        t.setId(UUID.randomUUID());
        t.setIsForSale(true);
        t.setCancellationPenalityBasedOnItemPrice(true);
        t.setAgreementFirstDelay(60);
        t.setAgreementSecondDelay(30);
        t.setWithdrawalDelay(48);
        t.setInterventionMinimumDelai(2);
        t.setMaxVisitNumber(4);
        t.setDelayLawyerMinute(60);
        t.setDelayMinuteAllSettled(30);
        t.setEcarttypeConcretisation(BigDecimal.valueOf(5));
        t.setEcarttypeFinalisation(BigDecimal.valueOf(3));
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
        t.setSavAddress("");
        zeros(t);
        return t;
    }

    private static void zeros(CfgTranche t) {
        BigDecimal z = BigDecimal.ZERO;
        t.setAdvancePercent(z);
        t.setBeforeReservationAmount(z);
        t.setBeforeReservationAmountDiscount(z);
        t.setSyndicAmount(z);
        t.setSyndicAmountDiscount(z);
        t.setAdvancePercentDiscount(z);
        t.setDefaultExpenseAmount(z);
        t.setDefaultExpenseAmountDiscount(z);
        t.setCancellationPenalityPercent(z);
        t.setCancellationPenalityPercentDiscount(z);
        t.setArticleTransferPenality(z);
        t.setArticleTransferPenalityDiscount(z);
        t.setSliceTransferPenality(z);
        t.setSliceTransferPenalityDiscount(z);
        t.setCustomerTransferPenality(z);
        t.setCustomerTransferPenalityDiscount(z);
        t.setOtherExpenseAmount(z);
        t.setOtherExpenseAmountDiscount(z);
        t.setAgreementFirstPenaltyPercent(z);
        t.setAgreementSecondPenaltyPercent(z);
        t.setTotalSaleBasePrevision(z);
        t.setBaseEcarttypeConcretisation(z);
        t.setBaseEcarttypeFinalisation(z);
        t.setBaseConcretisationPercent(z);
        t.setUnitConcretisation(z);
        t.setUnitFinalisation(z);
        t.setUnitTurnover(z);
        t.setUnitTurnoverHT(z);
        t.setCoveredFloorArea(z);
        t.setAdvanceAmount(z);
        t.setCreditPercent(z);
        t.setSyndicPeriod(0);
        t.setWarrantyDelay(0);
        t.setWarrantyDelayAdded(0);
        t.setPaymentBalanceDelay(0);
        t.setCountItemBasePrevision(0);
        t.setMaxEcheanceDelai(0);
    }

    private void appliquer(CfgTranche t, TrancheEcritureDto d) {
        ChampsAVider v = ChampsAVider.de(d, d.champsAVider(), OBLIGATOIRES);
        t.setCode(codeLibre(obligatoire(d.code(), t.getCode(), "Le code"), t.getId()));
        t.setDescription(obligatoire(d.description(), t.getDescription(), "La description"));
        t.setDescriptionLong(texte(v.texte("descriptionLongue", d.descriptionLongue()), t.getDescriptionLong(), 500,
                "La description longue"));
        visibilites(t, d, v);
        coordonnees(t, d, v);
        clesEtrangeres(t, d, v);
        dates(t, d, v);
        identificationFonciere(t, d, v);
        reglesDeGestion(t, d, v);
        remises(t, d, v);
        delais(t, d, v);
    }

    private static void visibilites(CfgTranche t, TrancheEcritureDto d, ChampsAVider v) {
        t.setIsForSale(v.ou("estEnVente", d.estEnVente(), t.getIsForSale()));
        t.setIsForRent(v.ou("estEnLocation", d.estEnLocation(), t.getIsForRent()));
        t.setIsForProject(v.ou("visibleProjet", d.visibleProjet(), t.getIsForProject()));
        t.setIsForRom(v.ou("visibleRom", d.visibleRom(), t.getIsForRom()));
        t.setIsForBuy(v.ou("visibleAchat", d.visibleAchat(), t.getIsForBuy()));
        t.setIsForSav(v.ou("visibleSav", d.visibleSav(), t.getIsForSav()));
        t.setIsForPrm(v.ou("visiblePrm", d.visiblePrm(), t.getIsForPrm()));
        t.setIsWithGiftForProposer(v.ou("cadeauParrain", d.cadeauParrain(), t.getIsWithGiftForProposer()));
        t.setIsWithGiftForOldCustomer(v.ou("cadeauAncienClient", d.cadeauAncienClient(), t.getIsWithGiftForOldCustomer()));
        t.setIsWithGiftForProspectionOrigin(v.ou("cadeauOrigineProspection", d.cadeauOrigineProspection(),
                t.getIsWithGiftForProspectionOrigin()));
        t.setAccumulatedGifts(v.ou("cadeauxCumules", d.cadeauxCumules(), t.getAccumulatedGifts()));
    }

    private void coordonnees(CfgTranche t, TrancheEcritureDto d, ChampsAVider v) {
        t.setAddress(texte(v.texte("adresse", d.adresse()), t.getAddress(), COURT, "L'adresse"));
        t.setAddress2(texte(v.texte("adresse2", d.adresse2()), t.getAddress2(), COURT, "L'adresse 2"));
        t.setAddress3(texte(v.texte("adresse3", d.adresse3()), t.getAddress3(), COURT, "L'adresse 3"));
        t.setZipCode(texte(v.texte("codePostal", d.codePostal()), t.getZipCode(), COURT, "Le code postal"));
        t.setCity(texte(v.texte("ville", d.ville()), t.getCity(), COURT, "La ville"));
        t.setPhone(texte(v.texte("telephone", d.telephone()), t.getPhone(), COURT, "Le téléphone"));
        t.setMobile(texte(v.texte("mobile", d.mobile()), t.getMobile(), COURT, "Le mobile"));
        t.setFax(texte(v.texte("fax", d.fax()), t.getFax(), COURT, "Le fax"));
        t.setEmail(texte(v.texte("email", d.email()), t.getEmail(), COURT, "L'email"));
        t.setWebSite(texte(v.texte("siteWeb", d.siteWeb()), t.getWebSite(), COURT, "Le site web"));
        t.setSavAddress(texte(v.texte("adresseSav", d.adresseSav()), t.getSavAddress(), COURT, "L'adresse SAV"));
        t.setCommerceTribunal(texte(v.texte("tribunalCommerce", d.tribunalCommerce()), t.getCommerceTribunal(), COURT,
                "Le tribunal de commerce"));
        t.setCancellationRightCode(texte(v.texte("codeDroitRetractation", d.codeDroitRetractation()),
                t.getCancellationRightCode(), COURT, "Le code du droit de rétractation"));
        t.setAdmCountry(v.reference("paysId", d.paysId(), t.getAdmCountry(),
                id -> ProjetService.pays(countryRepository, id)));
    }

    private void clesEtrangeres(CfgTranche t, TrancheEcritureDto d, ChampsAVider v) {
        t.setCfgTrancheStanding(v.reference("standingId", d.standingId(), t.getCfgTrancheStanding(),
                id -> references.standing(id, "standingId")));
        t.setLndLevel(v.reference("niveauTerrainId", d.niveauTerrainId(), t.getLndLevel(),
                id -> references.niveau(id, "niveauTerrainId")));
        t.setStkTrancheHeading(v.reference("rubriqueId", d.rubriqueId(), t.getStkTrancheHeading(),
                id -> references.rubrique(id, "rubriqueId")));
        t.setCfgClaimsManager(v.reference("responsableSavId", d.responsableSavId(), t.getCfgClaimsManager(),
                id -> references.tiers(id, "responsableSavId")));
        t.setCfgTierOther(v.reference("tiersAutruiId", d.tiersAutruiId(), t.getCfgTierOther(),
                id -> references.tiers(id, "tiersAutruiId")));
        t.setPrjProject(v.reference("projetFacturationId", d.projetFacturationId(), t.getPrjProject(),
                id -> projetDeLEntite(references.projet(id, "projetFacturationId"), t)));
        t.setActAccount(v.reference("planComptableId", d.planComptableId(), t.getActAccount(),
                id -> references.compte(id, "planComptableId")));
    }

    /** Tranche.cs : le sélecteur ne propose que les projets de l'entité du programme. */
    private static PrjProject projetDeLEntite(PrjProject projet, CfgTranche t) {
        UUID entite = Champs.de(Champs.de(t.getCfgProject(), CfgProject::getCfgCompany), CfgCompany::getId);
        if (entite == null || !entite.equals(Champs.de(projet.getCfgCompany(), CfgCompany::getId))) {
            throw new InvalidRequestException("projetFacturationId : le projet n'appartient pas à l'entité du programme.");
        }
        return projet;
    }

    private static void dates(CfgTranche t, TrancheEcritureDto d, ChampsAVider v) {
        t.setWorkStartDate(v.ou("dateDebutTravaux", d.dateDebutTravaux(), t.getWorkStartDate()));
        t.setWorkEndDate(v.ou("dateFinTravaux", d.dateFinTravaux(), t.getWorkEndDate()));
        t.setMarketingStartDate(v.ou("dateDebutCommercialisation", d.dateDebutCommercialisation(),
                t.getMarketingStartDate()));
        t.setMarketingEndDate(v.ou("dateFinCommercialisation", d.dateFinCommercialisation(), t.getMarketingEndDate()));
        t.setDeliveryDate(v.ou("dateOctroiTitresFonciers", d.dateOctroiTitresFonciers(), t.getDeliveryDate()));
        t.setOccupancyPermitsDate(v.ou("datePermisHabiter", d.datePermisHabiter(), t.getOccupancyPermitsDate()));
        t.setReceptionDate(v.ou("dateReception", d.dateReception(), t.getReceptionDate()));
        t.setRevival1Date(v.ou("datePremiereRelance", d.datePremiereRelance(), t.getRevival1Date()));
        t.setEndDateConcretisation(v.ou("dateFinConcretisation", d.dateFinConcretisation(), t.getEndDateConcretisation()));
        t.setStartDateFinalisation(v.ou("dateDebutFinalisation", d.dateDebutFinalisation(),
                t.getStartDateFinalisation()));
        ordre(t.getWorkStartDate(), t.getWorkEndDate(), "Travaux");
        ordre(t.getMarketingStartDate(), t.getMarketingEndDate(), "Commercialisation");
    }

    private static void identificationFonciere(CfgTranche t, TrancheEcritureDto d, ChampsAVider v) {
        t.setRaisedHandDateAFH(v.ou("dateMainLeveeAfh", d.dateMainLeveeAfh(), t.getRaisedHandDateAFH()));
        t.setTitleDate1(v.ou("dateCertificatDefinitif1", d.dateCertificatDefinitif1(), t.getTitleDate1()));
        t.setRaisedHandDateBank(v.ou("dateMainLeveeBancaire", d.dateMainLeveeBancaire(), t.getRaisedHandDateBank()));
        t.setTitleDate2(v.ou("dateCertificatDefinitif2", d.dateCertificatDefinitif2(), t.getTitleDate2()));
        t.setMunicipalityDateDeposit(v.ou("dateDepotLotissementMunicipalite", d.dateDepotLotissementMunicipalite(),
                t.getMunicipalityDateDeposit()));
        t.setMunicipalityDateWithdrawal(v.ou("dateRetraitLotissementMunicipalite",
                d.dateRetraitLotissementMunicipalite(), t.getMunicipalityDateWithdrawal()));
        t.setOTCDateNonApprovedDeposit(v.ou("dateDepotLotissementNonApprouveOtc",
                d.dateDepotLotissementNonApprouveOtc(), t.getOTCDateNonApprovedDeposit()));
        t.setOTCDateSettlement(v.ou("dateReglementFraisOtc", d.dateReglementFraisOtc(), t.getOTCDateSettlement()));
        t.setOTCDateApprovedDeposit(v.ou("dateDepotLotissementApprouveOtc", d.dateDepotLotissementApprouveOtc(),
                t.getOTCDateApprovedDeposit()));
        t.setOTCDateMutationWithdrawal(v.ou("dateRetraitPlansMutationOtc", d.dateRetraitPlansMutationOtc(),
                t.getOTCDateMutationWithdrawal()));
        t.setSharingDate(v.ou("dateAchevementPartages", d.dateAchevementPartages(), t.getSharingDate()));
        t.setSharingDateDeposit(v.ou("dateDepotPartageCpf", d.dateDepotPartageCpf(), t.getSharingDateDeposit()));
        t.setCPFSharingSettlementDate(v.ou("datePaiementFraisPartageCpf", d.datePaiementFraisPartageCpf(),
                t.getCPFSharingSettlementDate()));
        t.setCPFSharingWithdrawalDate(v.ou("dateRetraitPartageCpf", d.dateRetraitPartageCpf(),
                t.getCPFSharingWithdrawalDate()));
        t.setTitleDate(v.ou("dateRetraitCertificatsIndividuels", d.dateRetraitCertificatsIndividuels(),
                t.getTitleDate()));
        t.setTitleDateVerification(v.ou("dateVerificationCertificats", d.dateVerificationCertificats(),
                t.getTitleDateVerification()));
        t.setRepartitionDate(v.ou("dateRepartitionCharges", d.dateRepartitionCharges(), t.getRepartitionDate()));
    }

    private static void reglesDeGestion(CfgTranche t, TrancheEcritureDto d, ChampsAVider v) {
        t.setBeforeReservationAmount(v.ou("montantPreReservation",
                positif(d.montantPreReservation(), "Montant pré-réservation"), t.getBeforeReservationAmount()));
        t.setAdvanceBasedOnAmount(v.ou("avanceSurMontant", d.avanceSurMontant(), t.getAdvanceBasedOnAmount()));
        t.setAdvanceAmount(v.ou("montantAvance", positif(d.montantAvance(), "Montant de l'avance"), t.getAdvanceAmount()));
        t.setAdvancePercent(v.ou("advancePercent", fraction(d.advancePercent(), "Concrétisation"), t.getAdvancePercent()));
        // OMRAN écrit ConcretisationPercent par accesseur ; SchemaFinancementCalculator lit AdvancePercent.
        t.setConcretisationPercent(t.getAdvancePercent());
        t.setDefaultExpenseAmount(v.ou("fraisDossier", positif(d.fraisDossier(), "Frais de dossier"),
                t.getDefaultExpenseAmount()));
        t.setCancellationPenalityPercent(v.ou("tauxPenaliteAnnulation",
                fraction(d.tauxPenaliteAnnulation(), "Pénalité d'annulation"), t.getCancellationPenalityPercent()));
        t.setCancellationPenalityBasedOnItemPrice(v.ou("penaliteAnnulationSurPrixArticle",
                d.penaliteAnnulationSurPrixArticle(), t.getCancellationPenalityBasedOnItemPrice()));
        t.setArticleTransferPenality(v.ou("penaliteTransfertArticle",
                positif(d.penaliteTransfertArticle(), "Pénalité transfert article"), t.getArticleTransferPenality()));
        t.setSliceTransferPenality(v.ou("penaliteTransfertTranche",
                positif(d.penaliteTransfertTranche(), "Pénalité transfert tranche"), t.getSliceTransferPenality()));
        t.setCustomerTransferPenality(v.ou("penaliteTransfertClient",
                positif(d.penaliteTransfertClient(), "Pénalité transfert client"), t.getCustomerTransferPenality()));
        t.setOtherExpenseAmount(v.ou("autresFrais", positif(d.autresFrais(), "Autres frais"), t.getOtherExpenseAmount()));
        t.setSyndicAmount(v.ou("montantSyndic", positif(d.montantSyndic(), "Montant syndic"), t.getSyndicAmount()));
        t.setSyndicPeriod(v.ou("periodeSyndic", positif(d.periodeSyndic(), "Période syndic"), t.getSyndicPeriod()));
        t.setAgreementFirstPenaltyPercent(v.ou("tauxPenalitePremierEngagement",
                fraction(d.tauxPenalitePremierEngagement(), "Pénalité 1er engagement"), t.getAgreementFirstPenaltyPercent()));
        t.setAgreementSecondPenaltyPercent(v.ou("tauxPenaliteDeuxiemeEngagement",
                fraction(d.tauxPenaliteDeuxiemeEngagement(), "Pénalité 2e engagement"),
                t.getAgreementSecondPenaltyPercent()));
        if (d.creditPercent() != null && d.creditPercent().compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidRequestException("Le crédit max ne doit pas dépasser 100% !");
        }
        t.setCreditPercent(v.ou("creditPercent", fraction(d.creditPercent(), "Crédit max"), t.getCreditPercent()));
        t.setIsWithVerifCredit(v.ou("isWithVerifCredit", d.isWithVerifCredit(), t.getIsWithVerifCredit()));
        t.setEcarttypeConcretisation(v.ou("ecartTypeConcretisation",
                positif(d.ecartTypeConcretisation(), "Écart type concrétisation"), t.getEcarttypeConcretisation()));
        t.setEcarttypeFinalisation(v.ou("ecartTypeFinalisation",
                positif(d.ecartTypeFinalisation(), "Écart type finalisation"), t.getEcarttypeFinalisation()));
    }

    private static void remises(CfgTranche t, TrancheEcritureDto d, ChampsAVider v) {
        t.setBeforeReservationAmountDiscount(v.ou("montantPreReservationRemise",
                positif(d.montantPreReservationRemise(), "Montant pré-réservation (remise)"),
                t.getBeforeReservationAmountDiscount()));
        t.setAdvancePercentDiscount(v.ou("advancePercentRemise",
                fraction(d.advancePercentRemise(), "Concrétisation (remise)"), t.getAdvancePercentDiscount()));
        t.setIsActiveAdvancePercentDiscount(v.ou("advancePercentRemiseActive", d.advancePercentRemiseActive(),
                t.getIsActiveAdvancePercentDiscount()));
        t.setDefaultExpenseAmountDiscount(v.ou("fraisDossierRemise",
                positif(d.fraisDossierRemise(), "Frais de dossier (remise)"), t.getDefaultExpenseAmountDiscount()));
        t.setCancellationPenalityPercentDiscount(v.ou("tauxPenaliteAnnulationRemise",
                fraction(d.tauxPenaliteAnnulationRemise(), "Pénalité d'annulation (remise)"),
                t.getCancellationPenalityPercentDiscount()));
        t.setArticleTransferPenalityDiscount(v.ou("penaliteTransfertArticleRemise",
                positif(d.penaliteTransfertArticleRemise(), "Pénalité transfert article (remise)"),
                t.getArticleTransferPenalityDiscount()));
        t.setSliceTransferPenalityDiscount(v.ou("penaliteTransfertTrancheRemise",
                positif(d.penaliteTransfertTrancheRemise(), "Pénalité transfert tranche (remise)"),
                t.getSliceTransferPenalityDiscount()));
        t.setCustomerTransferPenalityDiscount(v.ou("penaliteTransfertClientRemise",
                positif(d.penaliteTransfertClientRemise(), "Pénalité transfert client (remise)"),
                t.getCustomerTransferPenalityDiscount()));
        t.setOtherExpenseAmountDiscount(v.ou("autresFraisRemise",
                positif(d.autresFraisRemise(), "Autres frais (remise)"), t.getOtherExpenseAmountDiscount()));
        t.setSyndicAmountDiscount(v.ou("montantSyndicRemise",
                positif(d.montantSyndicRemise(), "Montant syndic (remise)"), t.getSyndicAmountDiscount()));
    }

    private static void delais(CfgTranche t, TrancheEcritureDto d, ChampsAVider v) {
        t.setAgreementFirstDelay(v.ou("delaiPremierEngagement",
                positif(d.delaiPremierEngagement(), "Délai 1er engagement"), t.getAgreementFirstDelay()));
        t.setAgreementSecondDelay(v.ou("delaiDeuxiemeEngagement",
                positif(d.delaiDeuxiemeEngagement(), "Délai 2e engagement"), t.getAgreementSecondDelay()));
        t.setPaymentBalanceDelay(v.ou("delaiSoldePaiement",
                positif(d.delaiSoldePaiement(), "Délai solde paiement"), t.getPaymentBalanceDelay()));
        t.setWarrantyDelay(v.ou("delaiGarantie", positif(d.delaiGarantie(), "Délai de garantie"), t.getWarrantyDelay()));
        t.setWarrantyDelayAdded(v.ou("delaiGarantieSupplement",
                positif(d.delaiGarantieSupplement(), "Supplément de garantie"), t.getWarrantyDelayAdded()));
        t.setInterventionMinimumDelai(v.ou("delaiMinimumIntervention",
                positif(d.delaiMinimumIntervention(), "Délai minimum d'intervention"), t.getInterventionMinimumDelai()));
        t.setMaxVisitNumber(v.ou("maxVisitNumber", positif(d.maxVisitNumber(), "Nombre maximum de visites"),
                t.getMaxVisitNumber()));
        t.setWithdrawalDelay(v.ou("delaiRetractation", positif(d.delaiRetractation(), "Délai de rétractation"),
                t.getWithdrawalDelay()));
        t.setMaxEcheanceDelai(v.ou("delaiMaxEcheance", positif(d.delaiMaxEcheance(), "Délai max échéance"),
                t.getMaxEcheanceDelai()));
        t.setIsWithVerifMaxEcheanceDelai(v.ou("verifierDelaiMaxEcheance", d.verifierDelaiMaxEcheance(),
                t.getIsWithVerifMaxEcheanceDelai()));
        t.setDelayLawyerMinute(v.ou("delaiNotaireMinutes", positif(d.delaiNotaireMinutes(), "Délai notaire"),
                t.getDelayLawyerMinute()));
        t.setDelayMinuteAllSettled(v.ou("delaiToutSoldeMinutes", positif(d.delaiToutSoldeMinutes(), "Délai tout soldé"),
                t.getDelayMinuteAllSettled()));
    }

    private String codeLibre(String code, UUID id) {
        trancheRepository.findFirstByCode(code)
                .filter(existante -> !existante.getId().equals(id))
                .ifPresent(existante -> {
                    throw new ConflictException("Le code %s est déjà pris.".formatted(code));
                });
        return code;
    }

    private static <T> T si(Set<ColonneTranche> colonnes, ColonneTranche colonne, Supplier<T> valeur) {
        return colonnes.contains(colonne) ? valeur.get() : null;
    }

    private static String libelle(CfgTier tiers) {
        return Champs.de(tiers, CfgTier::getDescription);
    }

    static TrancheListItemDto toListItem(CfgTranche t, Set<ColonneTranche> c) {
        CfgProject p = t.getCfgProject();
        return new TrancheListItemDto(
                t.getId(), Champs.de(p, CfgProject::getId), t.getCode(), t.getDescription(),
                Champs.de(p, CfgProject::getCode), t.getCity(), t.getDeliveryDate(),
                si(c, ColonneTranche.STANDING, () -> Champs.de(t.getCfgTrancheStanding(), CfgTrancheStanding::getDescription)),
                si(c, ColonneTranche.ENTITE, () -> Champs.de(Champs.de(p, CfgProject::getCfgCompany), CfgCompany::getCode)),
                t.getIsForSale(),
                t.getDescriptionLong(),
                si(c, ColonneTranche.NIVEAU_TERRAIN, () -> Champs.de(t.getLndLevel(), LndLevel::getDescription)),
                si(c, ColonneTranche.RUBRIQUE, () -> Champs.de(t.getStkTrancheHeading(), StkTrancheHeading::getDescription)),
                t.getCoveredFloorArea(),
                si(c, ColonneTranche.RESPONSABLE_SAV, () -> libelle(t.getCfgClaimsManager())),
                si(c, ColonneTranche.VILLE_SIGNATURE, () -> Champs.de(p, CfgProject::getSignatureCity)),
                si(c, ColonneTranche.SIGNATAIRE, () -> libelle(Champs.de(p, CfgProject::getAdmSignatory))),
                si(c, ColonneTranche.REPRESENTANT_LEGAL, () -> libelle(Champs.de(Champs.de(p, CfgProject::getCfgCompany),
                        CfgCompany::getRepresentantLegal))),
                si(c, ColonneTranche.RESPONSABLE_COMMERCIAL, () -> libelle(Champs.de(p, CfgProject::getCfgSalesManager))),
                si(c, ColonneTranche.RESPONSABLE_TECHNIQUE,
                        () -> libelle(Champs.de(p, CfgProject::getCfgTechnicalManager))),
                t.getWorkStartDate(), t.getWorkEndDate(), t.getOccupancyPermitsDate(), t.getRevival1Date(),
                t.getMarketingStartDate(), t.getEndDateConcretisation(), t.getStartDateFinalisation(),
                t.getMarketingEndDate(),
                t.getAddress(), t.getSavAddress(),
                si(c, ColonneTranche.PAYS, () -> Champs.de(t.getAdmCountry(), AdmCountry::getDescription)),
                t.getBeforeReservationAmount(), t.getAdvancePercent(), t.getDefaultExpenseAmount(),
                t.getCancellationPenalityPercent(), t.getCancellationPenalityBasedOnItemPrice(),
                t.getArticleTransferPenality(), t.getSliceTransferPenality(), t.getCustomerTransferPenality(),
                t.getOtherExpenseAmount(), t.getSyndicAmount(), t.getSyndicPeriod(),
                t.getAgreementFirstDelay(), t.getAgreementSecondDelay(), t.getAgreementFirstPenaltyPercent(),
                t.getAgreementSecondPenaltyPercent(), t.getPaymentBalanceDelay(), t.getWarrantyDelay(),
                t.getWarrantyDelayAdded(), t.getCancellationRightCode(), t.getCommerceTribunal(),
                t.getInterventionMinimumDelai(), t.getMaxVisitNumber(), t.getIsWithGiftForProposer(),
                t.getIsWithGiftForOldCustomer(), t.getIsWithGiftForProspectionOrigin(), t.getAccumulatedGifts(),
                t.getBeforeReservationAmountDiscount(), t.getAdvancePercentDiscount(),
                t.getIsActiveAdvancePercentDiscount(), t.getDefaultExpenseAmountDiscount(),
                t.getCancellationPenalityPercentDiscount(), t.getArticleTransferPenalityDiscount(),
                t.getSliceTransferPenalityDiscount(), t.getCustomerTransferPenalityDiscount(),
                t.getSyndicAmountDiscount(), t.getOtherExpenseAmountDiscount(),
                t.getDelayLawyerMinute(), t.getDelayMinuteAllSettled(), t.getEcarttypeConcretisation(),
                t.getEcarttypeFinalisation(),
                si(c, ColonneTranche.TIERS_AUTRUI, () -> libelle(t.getCfgTierOther())),
                si(c, ColonneTranche.PROJET_FACTURATION, () -> Champs.de(t.getPrjProject(), PrjProject::getDescription)),
                si(c, ColonneTranche.PLAN_COMPTABLE, () -> Champs.de(t.getActAccount(), ActAccount::getDescription)),
                t.getIsForRent(), t.getIsForProject(), t.getIsForBuy(), t.getIsForSav(), t.getIsForRom(),
                t.getIsForPrm(),
                t.getUserCreate(), t.getUserCreateDateTime(), t.getUserUpdate(), t.getUserUpdateDateTime());
    }

    private static TrancheDetailDto toDetail(CfgTranche tranche) {
        CfgProject projet = tranche.getCfgProject();
        CfgCompany entite = Champs.de(projet, CfgProject::getCfgCompany);
        AdmCountry pays = tranche.getAdmCountry();
        return new TrancheDetailDto(
                tranche.getId(),
                Champs.de(projet, CfgProject::getId),
                tranche.getCode(),
                tranche.getDescription(),
                tranche.getDescriptionLong(),
                tranche.getAddress(),
                tranche.getAddress2(),
                tranche.getAddress3(),
                tranche.getZipCode(),
                tranche.getCity(),
                tranche.getPhone(),
                tranche.getMobile(),
                tranche.getFax(),
                tranche.getEmail(),
                tranche.getWebSite(),
                tranche.getWorkStartDate(),
                tranche.getWorkEndDate(),
                tranche.getMarketingStartDate(),
                tranche.getMarketingEndDate(),
                tranche.getDeliveryDate(),
                tranche.getIsForSale(),
                tranche.getIsForRent(),

                tranche.getCancellationPenalityPercent(),
                tranche.getCancellationPenalityPercentDiscount(),
                tranche.getArticleTransferPenality(),
                tranche.getArticleTransferPenalityDiscount(),
                tranche.getSliceTransferPenality(),
                tranche.getSliceTransferPenalityDiscount(),
                tranche.getCustomerTransferPenality(),
                tranche.getCustomerTransferPenalityDiscount(),
                tranche.getAgreementFirstPenaltyPercent(),
                tranche.getAgreementSecondPenaltyPercent(),

                tranche.getWarrantyDelay(),
                tranche.getWarrantyDelayAdded(),
                tranche.getAgreementFirstDelay(),
                tranche.getAgreementSecondDelay(),
                tranche.getPaymentBalanceDelay(),
                tranche.getDelayLawyerMinute(),
                tranche.getDelayMinuteAllSettled(),
                tranche.getWithdrawalDelay(),
                tranche.getInterventionMinimumDelai(),
                tranche.getMaxEcheanceDelai(),

                tranche.getAdvancePercent(),
                tranche.getConcretisationPercent(),
                tranche.getMaxVisitNumber(),
                tranche.getIsWithVerifCredit(),
                tranche.getCreditPercent(),

                Champs.de(projet, CfgProject::getCode),
                Champs.de(entite, CfgCompany::getId),
                Champs.de(entite, CfgCompany::getCode),
                Champs.de(pays, AdmCountry::getId),
                Champs.de(pays, AdmCountry::getDescription),
                tranche.getOccupancyPermitsDate(),
                tranche.getReceptionDate(),
                tranche.getRevival1Date(),
                tranche.getEndDateConcretisation(),
                tranche.getStartDateFinalisation(),
                tranche.getBeforeReservationAmount(),
                tranche.getBeforeReservationAmountDiscount(),
                tranche.getAdvanceBasedOnAmount(),
                tranche.getAdvanceAmount(),
                tranche.getAdvancePercentDiscount(),
                tranche.getIsActiveAdvancePercentDiscount(),
                tranche.getDefaultExpenseAmount(),
                tranche.getDefaultExpenseAmountDiscount(),
                tranche.getCancellationPenalityBasedOnItemPrice(),
                tranche.getOtherExpenseAmount(),
                tranche.getOtherExpenseAmountDiscount(),
                tranche.getSyndicAmount(),
                tranche.getSyndicAmountDiscount(),
                tranche.getSyndicPeriod(),
                tranche.getCancellationRightCode(),
                tranche.getCommerceTribunal(),
                tranche.getIsWithVerifMaxEcheanceDelai(),
                tranche.getEcarttypeConcretisation(),
                tranche.getEcarttypeFinalisation(),
                tranche.getUserCreate(),
                tranche.getUserCreateDateTime(),
                tranche.getUserUpdate(),
                tranche.getUserUpdateDateTime(),

                Champs.de(tranche.getCfgTrancheStanding(), CfgTrancheStanding::getId),
                Champs.de(tranche.getCfgTrancheStanding(), CfgTrancheStanding::getDescription),
                Champs.de(tranche.getLndLevel(), LndLevel::getId),
                Champs.de(tranche.getLndLevel(), LndLevel::getDescription),
                Champs.de(tranche.getStkTrancheHeading(), StkTrancheHeading::getId),
                Champs.de(tranche.getStkTrancheHeading(), StkTrancheHeading::getDescription),
                Champs.de(tranche.getCfgClaimsManager(), CfgTier::getId),
                libelle(tranche.getCfgClaimsManager()),
                tranche.getSavAddress(),
                tranche.getIsForProject(),
                tranche.getIsForRom(),
                tranche.getIsForBuy(),
                tranche.getIsForSav(),
                tranche.getIsForPrm(),
                tranche.getIsWithGiftForProposer(),
                tranche.getIsWithGiftForOldCustomer(),
                tranche.getIsWithGiftForProspectionOrigin(),
                tranche.getAccumulatedGifts(),
                Champs.de(tranche.getCfgTierOther(), CfgTier::getId),
                libelle(tranche.getCfgTierOther()),
                Champs.de(tranche.getPrjProject(), PrjProject::getId),
                Champs.de(tranche.getPrjProject(), PrjProject::getDescription),
                Champs.de(tranche.getActAccount(), ActAccount::getId),
                Champs.de(tranche.getActAccount(), ActAccount::getDescription),
                tranche.getCoveredFloorArea(),
                tranche.getRaisedHandDateAFH(),
                tranche.getTitleDate1(),
                tranche.getRaisedHandDateBank(),
                tranche.getTitleDate2(),
                tranche.getMunicipalityDateDeposit(),
                tranche.getMunicipalityDateWithdrawal(),
                tranche.getOTCDateNonApprovedDeposit(),
                tranche.getOTCDateSettlement(),
                tranche.getOTCDateApprovedDeposit(),
                tranche.getOTCDateMutationWithdrawal(),
                tranche.getSharingDate(),
                tranche.getSharingDateDeposit(),
                tranche.getCPFSharingSettlementDate(),
                tranche.getCPFSharingWithdrawalDate(),
                tranche.getTitleDate(),
                tranche.getTitleDateVerification(),
                tranche.getRepartitionDate()
        );
    }
}
