package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.dto.patrimoine.TrancheEcritureDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.COURT;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.fraction;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ordre;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;

@Service
@Transactional(readOnly = true)
public class TrancheService {

    private static final String INTROUVABLE = "Tranche introuvable: ";

    private final CfgTrancheRepository trancheRepository;
    private final CfgProjectRepository projectRepository;
    private final AdmCountryRepository countryRepository;
    private final TracePatrimoine trace;

    public TrancheService(CfgTrancheRepository trancheRepository, CfgProjectRepository projectRepository,
                          AdmCountryRepository countryRepository, TracePatrimoine trace) {
        this.trancheRepository = trancheRepository;
        this.projectRepository = projectRepository;
        this.countryRepository = countryRepository;
        this.trace = trace;
    }

    public Page<TrancheListItemDto> findAll(UUID projetId, Pageable pageable) {
        Page<CfgTranche> page = projetId != null
                ? trancheRepository.findDesEntitesActivesParProjet(projetId, pageable)
                : trancheRepository.findDesEntitesActives(pageable);
        return page.map(TrancheService::toListItem);
    }

    static TrancheListItemDto toListItem(CfgTranche tranche) {
        CfgProject projet = tranche.getCfgProject();
        return new TrancheListItemDto(
                tranche.getId(),
                tranche.getCode(),
                tranche.getDescription(),
                tranche.getCity(),
                tranche.getDeliveryDate(),
                Champs.de(projet, CfgProject::getId),
                Champs.de(projet, CfgProject::getCode)
        );
    }

    public TrancheDetailDto findById(UUID id) {
        return trancheRepository.findById(id)
                .map(TrancheService::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException(INTROUVABLE + id));
    }

    @Transactional
    public TrancheDetailDto creerTranche(TrancheEcritureDto saisie) {
        if (saisie.projetId() == null) {
            throw new InvalidRequestException("Le programme est obligatoire.");
        }
        CfgProject projet = projectRepository.findById(saisie.projetId())
                .orElseThrow(() -> new InvalidRequestException("Programme inconnu : " + saisie.projetId()));
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
        CfgTranche tranche = trancheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(INTROUVABLE + id));
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
        t.setCode(codeLibre(obligatoire(d.code(), t.getCode(), "Le code"), t.getId()));
        t.setDescription(obligatoire(d.description(), t.getDescription(), "La description"));
        t.setDescriptionLong(texte(d.descriptionLongue(), t.getDescriptionLong(), 500, "La description longue"));
        t.setIsForSale(ou(d.estEnVente(), t.getIsForSale()));
        t.setIsForRent(ou(d.estEnLocation(), t.getIsForRent()));
        coordonnees(t, d);
        dates(t, d);
        reglesDeGestion(t, d);
        delais(t, d);
    }

    private void coordonnees(CfgTranche t, TrancheEcritureDto d) {
        t.setAddress(texte(d.adresse(), t.getAddress(), COURT, "L'adresse"));
        t.setAddress2(texte(d.adresse2(), t.getAddress2(), COURT, "L'adresse 2"));
        t.setAddress3(texte(d.adresse3(), t.getAddress3(), COURT, "L'adresse 3"));
        t.setZipCode(texte(d.codePostal(), t.getZipCode(), COURT, "Le code postal"));
        t.setCity(texte(d.ville(), t.getCity(), COURT, "La ville"));
        t.setPhone(texte(d.telephone(), t.getPhone(), COURT, "Le téléphone"));
        t.setMobile(texte(d.mobile(), t.getMobile(), COURT, "Le mobile"));
        t.setFax(texte(d.fax(), t.getFax(), COURT, "Le fax"));
        t.setEmail(texte(d.email(), t.getEmail(), COURT, "L'email"));
        t.setWebSite(texte(d.siteWeb(), t.getWebSite(), COURT, "Le site web"));
        t.setCommerceTribunal(texte(d.tribunalCommerce(), t.getCommerceTribunal(), COURT, "Le tribunal de commerce"));
        t.setCancellationRightCode(texte(d.codeDroitRetractation(), t.getCancellationRightCode(), COURT,
                "Le code du droit de rétractation"));
        if (d.paysId() != null) {
            t.setAdmCountry(ProjetService.pays(countryRepository, d.paysId()));
        }
    }

    private static void dates(CfgTranche t, TrancheEcritureDto d) {
        t.setWorkStartDate(ou(d.dateDebutTravaux(), t.getWorkStartDate()));
        t.setWorkEndDate(ou(d.dateFinTravaux(), t.getWorkEndDate()));
        t.setMarketingStartDate(ou(d.dateDebutCommercialisation(), t.getMarketingStartDate()));
        t.setMarketingEndDate(ou(d.dateFinCommercialisation(), t.getMarketingEndDate()));
        t.setDeliveryDate(ou(d.dateOctroiTitresFonciers(), t.getDeliveryDate()));
        t.setOccupancyPermitsDate(ou(d.datePermisHabiter(), t.getOccupancyPermitsDate()));
        t.setReceptionDate(ou(d.dateReception(), t.getReceptionDate()));
        t.setRevival1Date(ou(d.datePremiereRelance(), t.getRevival1Date()));
        t.setEndDateConcretisation(ou(d.dateFinConcretisation(), t.getEndDateConcretisation()));
        t.setStartDateFinalisation(ou(d.dateDebutFinalisation(), t.getStartDateFinalisation()));
        ordre(t.getWorkStartDate(), t.getWorkEndDate(), "Travaux");
        ordre(t.getMarketingStartDate(), t.getMarketingEndDate(), "Commercialisation");
    }

    private static void reglesDeGestion(CfgTranche t, TrancheEcritureDto d) {
        t.setBeforeReservationAmount(ou(positif(d.montantPreReservation(), "Montant pré-réservation"), t.getBeforeReservationAmount()));
        t.setBeforeReservationAmountDiscount(ou(positif(d.montantPreReservationRemise(), "Montant pré-réservation (remise)"),
                t.getBeforeReservationAmountDiscount()));
        t.setAdvanceBasedOnAmount(ou(d.avanceSurMontant(), t.getAdvanceBasedOnAmount()));
        t.setAdvanceAmount(ou(positif(d.montantAvance(), "Montant de l'avance"), t.getAdvanceAmount()));
        t.setAdvancePercent(ou(fraction(d.advancePercent(), "Concrétisation"), t.getAdvancePercent()));
        // OMRAN écrit ConcretisationPercent par accesseur ; SchemaFinancementCalculator lit AdvancePercent.
        t.setConcretisationPercent(t.getAdvancePercent());
        t.setAdvancePercentDiscount(ou(fraction(d.advancePercentRemise(), "Concrétisation (remise)"), t.getAdvancePercentDiscount()));
        t.setIsActiveAdvancePercentDiscount(ou(d.advancePercentRemiseActive(), t.getIsActiveAdvancePercentDiscount()));
        t.setDefaultExpenseAmount(ou(positif(d.fraisDossier(), "Frais de dossier"), t.getDefaultExpenseAmount()));
        t.setDefaultExpenseAmountDiscount(ou(positif(d.fraisDossierRemise(), "Frais de dossier (remise)"),
                t.getDefaultExpenseAmountDiscount()));
        t.setCancellationPenalityPercent(ou(fraction(d.tauxPenaliteAnnulation(), "Pénalité d'annulation"),
                t.getCancellationPenalityPercent()));
        t.setCancellationPenalityPercentDiscount(ou(fraction(d.tauxPenaliteAnnulationRemise(), "Pénalité d'annulation (remise)"),
                t.getCancellationPenalityPercentDiscount()));
        t.setCancellationPenalityBasedOnItemPrice(ou(d.penaliteAnnulationSurPrixArticle(),
                t.getCancellationPenalityBasedOnItemPrice()));
        t.setArticleTransferPenality(ou(positif(d.penaliteTransfertArticle(), "Pénalité transfert article"),
                t.getArticleTransferPenality()));
        t.setArticleTransferPenalityDiscount(ou(positif(d.penaliteTransfertArticleRemise(), "Pénalité transfert article (remise)"),
                t.getArticleTransferPenalityDiscount()));
        t.setSliceTransferPenality(ou(positif(d.penaliteTransfertTranche(), "Pénalité transfert tranche"),
                t.getSliceTransferPenality()));
        t.setSliceTransferPenalityDiscount(ou(positif(d.penaliteTransfertTrancheRemise(), "Pénalité transfert tranche (remise)"),
                t.getSliceTransferPenalityDiscount()));
        t.setCustomerTransferPenality(ou(positif(d.penaliteTransfertClient(), "Pénalité transfert client"),
                t.getCustomerTransferPenality()));
        t.setCustomerTransferPenalityDiscount(ou(positif(d.penaliteTransfertClientRemise(), "Pénalité transfert client (remise)"),
                t.getCustomerTransferPenalityDiscount()));
        t.setOtherExpenseAmount(ou(positif(d.autresFrais(), "Autres frais"), t.getOtherExpenseAmount()));
        t.setOtherExpenseAmountDiscount(ou(positif(d.autresFraisRemise(), "Autres frais (remise)"), t.getOtherExpenseAmountDiscount()));
        t.setSyndicAmount(ou(positif(d.montantSyndic(), "Montant syndic"), t.getSyndicAmount()));
        t.setSyndicAmountDiscount(ou(positif(d.montantSyndicRemise(), "Montant syndic (remise)"), t.getSyndicAmountDiscount()));
        t.setSyndicPeriod(ou(positif(d.periodeSyndic(), "Période syndic"), t.getSyndicPeriod()));
        t.setAgreementFirstPenaltyPercent(ou(fraction(d.tauxPenalitePremierEngagement(), "Pénalité 1er engagement"),
                t.getAgreementFirstPenaltyPercent()));
        t.setAgreementSecondPenaltyPercent(ou(fraction(d.tauxPenaliteDeuxiemeEngagement(), "Pénalité 2e engagement"),
                t.getAgreementSecondPenaltyPercent()));
        if (d.creditPercent() != null && d.creditPercent().compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidRequestException("Le crédit max ne doit pas dépasser 100% !");
        }
        t.setCreditPercent(ou(fraction(d.creditPercent(), "Crédit max"), t.getCreditPercent()));
        t.setIsWithVerifCredit(ou(d.isWithVerifCredit(), t.getIsWithVerifCredit()));
        t.setEcarttypeConcretisation(ou(positif(d.ecartTypeConcretisation(), "Écart type concrétisation"),
                t.getEcarttypeConcretisation()));
        t.setEcarttypeFinalisation(ou(positif(d.ecartTypeFinalisation(), "Écart type finalisation"), t.getEcarttypeFinalisation()));
    }

    private static void delais(CfgTranche t, TrancheEcritureDto d) {
        t.setAgreementFirstDelay(ou(positif(d.delaiPremierEngagement(), "Délai 1er engagement"), t.getAgreementFirstDelay()));
        t.setAgreementSecondDelay(ou(positif(d.delaiDeuxiemeEngagement(), "Délai 2e engagement"), t.getAgreementSecondDelay()));
        t.setPaymentBalanceDelay(ou(positif(d.delaiSoldePaiement(), "Délai solde paiement"), t.getPaymentBalanceDelay()));
        t.setWarrantyDelay(ou(positif(d.delaiGarantie(), "Délai de garantie"), t.getWarrantyDelay()));
        t.setWarrantyDelayAdded(ou(positif(d.delaiGarantieSupplement(), "Supplément de garantie"), t.getWarrantyDelayAdded()));
        t.setInterventionMinimumDelai(ou(positif(d.delaiMinimumIntervention(), "Délai minimum d'intervention"),
                t.getInterventionMinimumDelai()));
        t.setMaxVisitNumber(ou(positif(d.maxVisitNumber(), "Nombre maximum de visites"), t.getMaxVisitNumber()));
        t.setWithdrawalDelay(ou(positif(d.delaiRetractation(), "Délai de rétractation"), t.getWithdrawalDelay()));
        t.setMaxEcheanceDelai(ou(positif(d.delaiMaxEcheance(), "Délai max échéance"), t.getMaxEcheanceDelai()));
        t.setIsWithVerifMaxEcheanceDelai(ou(d.verifierDelaiMaxEcheance(), t.getIsWithVerifMaxEcheanceDelai()));
        t.setDelayLawyerMinute(ou(positif(d.delaiNotaireMinutes(), "Délai notaire"), t.getDelayLawyerMinute()));
        t.setDelayMinuteAllSettled(ou(positif(d.delaiToutSoldeMinutes(), "Délai tout soldé"), t.getDelayMinuteAllSettled()));
    }

    private String codeLibre(String code, UUID id) {
        trancheRepository.findFirstByCode(code)
                .filter(existante -> !existante.getId().equals(id))
                .ifPresent(existante -> {
                    throw new ConflictException("Le code %s est déjà pris par la tranche %s — %s."
                            .formatted(code, existante.getCode(), existante.getDescription()));
                });
        return code;
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
                tranche.getUserUpdateDateTime()
        );
    }
}
