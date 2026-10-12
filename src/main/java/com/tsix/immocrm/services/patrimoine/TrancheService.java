package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.dto.patrimoine.TrancheEcritureDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.ChampEnConflitException;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.utils.Champs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TrancheService {

    private static final String TRANCHE_INTROUVABLE = "Tranche introuvable: ";
    private static final String CHAMP_PROJET = "projetId";
    private static final String CHAMP_CODE = "code";
    private static final String CHAMP_DESCRIPTION = "description";

    private final CfgTrancheRepository trancheRepository;
    private final CfgProjectRepository projectRepository;
    private final AdmCountryRepository countryRepository;
    private final HabilitationCreateur habilitationCreateur;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public TrancheService(CfgTrancheRepository trancheRepository, CfgProjectRepository projectRepository,
                          AdmCountryRepository countryRepository, HabilitationCreateur habilitationCreateur,
                          UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.trancheRepository = trancheRepository;
        this.projectRepository = projectRepository;
        this.countryRepository = countryRepository;
        this.habilitationCreateur = habilitationCreateur;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
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
                .orElseThrow(() -> new ResourceNotFoundException(TRANCHE_INTROUVABLE + id));
    }

    @Transactional
    public TrancheDetailDto create(TrancheEcritureDto demande) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        UUID projetId = ReglesSaisie.requis(demande.projetId(), CHAMP_PROJET, "Programme");
        tranche.setCfgProject(projectRepository.findById(projetId)
                .orElseThrow(() -> new ChampInvalideException(CHAMP_PROJET, "Programme introuvable : " + projetId)));
        tranche.setCode(codeLibre(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"), tranche.getId()));
        tranche.setDescription(ReglesSaisie.requis(demande.description(), CHAMP_DESCRIPTION, "Description"));
        SaisieTranche.initialiserDefauts(tranche);
        appliquerFacultatifs(tranche, demande);

        UtilisateurAffiche.Auteur auteur = utilisateurAffiche.courant();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        tranche.setUserCreate(auteur.nom());
        tranche.setUserCreateDateTime(maintenant);
        tranche.setUserUpdate(auteur.nom());
        tranche.setUserUpdateDateTime(maintenant);
        CfgTranche enregistree = trancheRepository.save(tranche);
        habilitationCreateur.surTranche(auteur.compte(), enregistree);
        return toDetail(enregistree);
    }

    @Transactional
    public TrancheDetailDto update(UUID id, TrancheEcritureDto demande) {
        CfgTranche tranche = trancheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TRANCHE_INTROUVABLE + id));
        UUID projetActuel = Champs.de(tranche.getCfgProject(), CfgProject::getId);
        if (demande.projetId() != null && !demande.projetId().equals(projetActuel)) {
            throw new ChampInvalideException(CHAMP_PROJET, "Le programme d'une tranche ne se modifie pas.");
        }
        if (demande.code() != null) {
            tranche.setCode(codeLibre(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"), id));
        }
        if (demande.description() != null) {
            tranche.setDescription(ReglesSaisie.requis(demande.description(), CHAMP_DESCRIPTION, "Description"));
        }
        appliquerFacultatifs(tranche, demande);

        tranche.setUserUpdate(utilisateurAffiche.courant().nom());
        tranche.setUserUpdateDateTime(LocalDateTime.now(clock));
        return toDetail(trancheRepository.save(tranche));
    }

    private void appliquerFacultatifs(CfgTranche tranche, TrancheEcritureDto demande) {
        SaisieTranche.appliquer(tranche, demande);
        if (demande.paysId() != null) {
            tranche.setAdmCountry(ProjetService.pays(countryRepository, demande.paysId()));
        }
    }

    private String codeLibre(String code, UUID id) {
        trancheRepository.findFirstByCodeAndIdNot(code, id).ifPresent(existante -> {
            throw new ChampEnConflitException(CHAMP_CODE, "Le code « %s » est déjà utilisé par la tranche « %s »."
                    .formatted(existante.getCode(), existante.getDescription()));
        });
        return code;
    }

    private static TrancheDetailDto toDetail(CfgTranche t) {
        CfgProject projet = t.getCfgProject();
        CfgCompany entite = Champs.de(projet, CfgProject::getCfgCompany);
        AdmCountry pays = t.getAdmCountry();
        return new TrancheDetailDto(
                t.getId(), Champs.de(projet, CfgProject::getId), t.getCode(), t.getDescription(),
                t.getDescriptionLong(), t.getAddress(), t.getAddress2(), t.getAddress3(), t.getZipCode(), t.getCity(),
                t.getPhone(), t.getMobile(), t.getFax(), t.getEmail(), t.getWebSite(),
                t.getWorkStartDate(), t.getWorkEndDate(), t.getMarketingStartDate(), t.getMarketingEndDate(),
                t.getDeliveryDate(), t.getIsForSale(), t.getIsForRent(),

                t.getCancellationPenalityPercent(), t.getCancellationPenalityPercentDiscount(),
                t.getArticleTransferPenality(), t.getArticleTransferPenalityDiscount(),
                t.getSliceTransferPenality(), t.getSliceTransferPenalityDiscount(),
                t.getCustomerTransferPenality(), t.getCustomerTransferPenalityDiscount(),
                t.getAgreementFirstPenaltyPercent(), t.getAgreementSecondPenaltyPercent(),

                t.getWarrantyDelay(), t.getWarrantyDelayAdded(), t.getAgreementFirstDelay(), t.getAgreementSecondDelay(),
                t.getPaymentBalanceDelay(), t.getDelayLawyerMinute(), t.getDelayMinuteAllSettled(),
                t.getWithdrawalDelay(), t.getInterventionMinimumDelai(), t.getMaxEcheanceDelai(),

                t.getAdvancePercent(), t.getConcretisationPercent(), t.getMaxVisitNumber(), t.getIsWithVerifCredit(),
                t.getCreditPercent(),

                t.getOccupancyPermitsDate(), t.getReceptionDate(), t.getRevival1Date(), t.getEndDateConcretisation(),
                t.getStartDateFinalisation(), t.getBeforeReservationAmount(), t.getBeforeReservationAmountDiscount(),
                t.getAdvanceBasedOnAmount(), t.getAdvanceAmount(), t.getAdvancePercentDiscount(),
                t.getIsActiveAdvancePercentDiscount(), t.getDefaultExpenseAmount(), t.getDefaultExpenseAmountDiscount(),
                t.getCancellationPenalityBasedOnItemPrice(), t.getOtherExpenseAmount(),
                t.getOtherExpenseAmountDiscount(), t.getSyndicAmount(), t.getSyndicAmountDiscount(),
                t.getSyndicPeriod(), t.getCancellationRightCode(), t.getCommerceTribunal(),
                t.getIsWithVerifMaxEcheanceDelai(), t.getEcarttypeConcretisation(), t.getEcarttypeFinalisation(),

                Champs.de(projet, CfgProject::getCode),
                Champs.de(entite, CfgCompany::getId),
                Champs.de(entite, CfgCompany::getCode),
                Champs.de(pays, AdmCountry::getId),
                Champs.de(pays, AdmCountry::getDescription),
                t.getUserCreate(), t.getUserCreateDateTime(), t.getUserUpdate(), t.getUserUpdateDateTime()
        );
    }
}
