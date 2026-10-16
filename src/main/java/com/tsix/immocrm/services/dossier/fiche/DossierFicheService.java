package com.tsix.immocrm.services.dossier.fiche;

import com.tsix.immocrm.domain.ActionsDossier;
import com.tsix.immocrm.domain.ConventionRemise;
import com.tsix.immocrm.dto.dossier.fiche.AnnulationFicheDto;
import com.tsix.immocrm.dto.dossier.fiche.AutresFicheDto;
import com.tsix.immocrm.dto.dossier.fiche.AutresMontantsFicheDto;
import com.tsix.immocrm.dto.dossier.fiche.DatesAutresFicheDto;
import com.tsix.immocrm.dto.dossier.fiche.DatesFicheDto;
import com.tsix.immocrm.dto.dossier.fiche.DossierFicheDto;
import com.tsix.immocrm.dto.dossier.fiche.EnTeteFicheDto;
import com.tsix.immocrm.dto.dossier.fiche.StatutFicheDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ActionDossierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Fiche complète du dossier en lecture ({@code GET /api/dossiers/{id}/fiche}), onglet par onglet de l'écran
 * OMRAN. Le périmètre est contrôlé en amont par {@code PerimetreInterceptor}. Voir FEATURE-vente-fiche.md §A.
 */
@Service
@Transactional(readOnly = true)
public class DossierFicheService {

    private static final String DOSSIER_INTROUVABLE = "Dossier introuvable: ";
    private static final int ECHELLE_PRIX_M2 = 6;

    private final ComFolderRepository folderRepository;
    private final ComFolderItemRepository folderItemRepository;
    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final ActionDossierRepository actionRepository;
    private final EntityManager entityManager;
    private final Clock clock;

    public DossierFicheService(ComFolderRepository folderRepository,
                               ComFolderItemRepository folderItemRepository,
                               ComDocumentRepository documentRepository,
                               StlSettlementLineRepository settlementLineRepository,
                               ActionDossierRepository actionRepository,
                               EntityManager entityManager,
                               Clock clock) {
        this.folderRepository = folderRepository;
        this.folderItemRepository = folderItemRepository;
        this.documentRepository = documentRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.actionRepository = actionRepository;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    public DossierFicheDto fiche(UUID dossierId) {
        ComFolder f = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId));
        LocalDateTime maintenant = LocalDateTime.now(clock);
        return new DossierFicheDto(
                f.getId(),
                f.getCode(),
                enTete(f),
                statut(f, maintenant),
                autresMontants(f, superficieVendue(f)),
                dates(f),
                datesAutres(f),
                new AnnulationFicheDto(f.getCancelDate(), f.getCancelDescription()),
                autres(f, decisionMeilleurTaux(dossierId)),
                aDesImpayes(dossierId, maintenant),
                instructionNonLue(dossierId));
    }

    /**
     * Une échéance échue (date dépassée) dont le reste dû est positif — même calcul du réglé que
     * l'échéancier (règlements annulés exclus).
     */
    boolean aDesImpayes(UUID dossierId, LocalDateTime maintenant) {
        Map<UUID, BigDecimal> regleParEcheance = new HashMap<>();
        for (Object[] ligne : settlementLineRepository.sumAmountByComFolderIdGroupedByComDocument(dossierId)) {
            regleParEcheance.put((UUID) ligne[0], (BigDecimal) ligne[1]);
        }
        return documentRepository.findByComFolderIdOrderByDocDateAsc(dossierId).stream()
                .filter(d -> d.getDocDate() != null && d.getDocDate().isBefore(maintenant))
                .anyMatch(d -> reste(d, regleParEcheance).signum() > 0);
    }

    private static BigDecimal reste(ComDocument d, Map<UUID, BigDecimal> regleParEcheance) {
        BigDecimal montant = Objects.requireNonNullElse(d.getAmount(), BigDecimal.ZERO);
        return montant.subtract(regleParEcheance.getOrDefault(d.getId(), BigDecimal.ZERO));
    }

    private boolean instructionNonLue(UUID dossierId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user) || user.employeeId() == null) {
            return false;
        }
        return actionRepository.instructionNonLue(dossierId, ActionsDossier.TYPE_INSTRUCTION, user.employeeId());
    }

    /**
     * {@code ComLoanCompanyDecisionId} n'est pas mappé sur {@code ComFolder} : lu en natif. La table de
     * libellés n'existe pas dans toutes les bases de test, d'où la seconde requête seulement si l'id est posé.
     */
    private String decisionMeilleurTaux(UUID dossierId) {
        List<?> ids = entityManager.createNativeQuery(
                        "SELECT ComLoanCompanyDecisionId FROM ComFolder WHERE PKey = :id AND ComLoanCompanyDecisionId IS NOT NULL")
                .setParameter("id", dossierId)
                .getResultList();
        if (ids.isEmpty()) {
            return null;
        }
        List<?> libelles = entityManager.createNativeQuery(
                        "SELECT Description FROM ComLoanCompanyDecision WHERE PKey = :id")
                .setParameter("id", ids.get(0))
                .getResultList();
        return libelles.isEmpty() ? null : (String) libelles.get(0);
    }

    private static EnTeteFicheDto enTete(ComFolder f) {
        return new EnTeteFicheDto(
                f.getArea(),
                f.getUnitCostHT(),
                f.getUnitCost(),
                f.getItemPriceHT(),
                f.getItemPrice(),
                f.getUnderItemsPriceHT(),
                f.getUnderItemsPrice(),
                f.getPriceBeforeDiscountHT(),
                f.getPriceBeforeDiscount(),
                f.getDiscountAmount(),
                ConventionRemise.pourcentage(f.getDiscount()),
                f.getPriceAfterDiscountHT(),
                f.getPriceAfterDiscount(),
                f.getAgreementPrice());
    }

    private static StatutFicheDto statut(ComFolder f, LocalDateTime maintenant) {
        LocalDateTime validite = f.getStatusValidityDate();
        return new StatutFicheDto(
                Champs.de(f.getStrStatus(), StrStatus::getCode),
                validite,
                validite != null && validite.isBefore(maintenant));
    }

    /**
     * Superficie sur laquelle OMRAN divise le prix de vente au m² : l'article et ses sous-articles qui ne
     * sont pas des « sous-articles d'origine » ({@code Sale.cs:ComSaleUnitCostHT}).
     */
    private BigDecimal superficieVendue(ComFolder f) {
        BigDecimal sousArticles = folderItemRepository.findByComFolderId(f.getId()).stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsUnderItem()))
                .filter(i -> !Boolean.TRUE.equals(i.getIsUnderItemOrigin()))
                .map(ComFolderItem::getArea)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Objects.requireNonNullElse(f.getArea(), BigDecimal.ZERO).add(sousArticles);
    }

    private static AutresMontantsFicheDto autresMontants(ComFolder f, BigDecimal superficieVendue) {
        return new AutresMontantsFicheDto(
                f.getArea(),
                f.getGlobalArea(),
                f.getVATRatio(),
                f.getUnitCostHT(),
                f.getUnitCost(),
                f.getItemPriceHT(),
                f.getItemPrice(),
                f.getUnderItemsPriceHT(),
                f.getUnderItemsPrice(),
                f.getItemPriceAfterHT(),
                f.getItemPriceAfter(),
                f.getUnderItemsPriceAfterHT(),
                f.getUnderItemsPriceAfter(),
                f.getPriceBeforeDiscountHT(),
                f.getPriceBeforeDiscount(),
                ConventionRemise.pourcentage(f.getDiscount()),
                f.getPriceAfterDiscountHT(),
                f.getPriceAfterDiscount(),
                parM2(f.getPriceAfterDiscountHT(), superficieVendue),
                parM2(f.getPriceAfterDiscount(), superficieVendue));
    }

    static BigDecimal parM2(BigDecimal prix, BigDecimal superficie) {
        if (prix == null || superficie == null || superficie.signum() == 0) {
            return null;
        }
        return prix.divide(superficie, ECHELLE_PRIX_M2, RoundingMode.HALF_UP);
    }

    private static DatesFicheDto dates(ComFolder f) {
        StkItem article = f.getStkItem();
        CfgTranche tranche = Champs.de(article, StkItem::getStkHierarchy);
        return new DatesFicheDto(
                f.getAdvanceDate(),
                f.getReservationDate(),
                f.getReceptionDate(),
                f.getAgreementDatePrevisionel(),
                f.getIsConfirmedAgreementDate(),
                f.getAgreementRegistryDate(),
                f.getAgreementDate(),
                f.getAgreementSavingDate(),
                f.getOwnershipCertificateDate(),
                f.getRevival1(),
                f.getRevival1ValidityDate(),
                f.getRevival2(),
                f.getRevival2ValidityDate(),
                f.getBankRecordDate(),
                f.getCreditApprovalDate(),
                f.getCreditPossessionDate(),
                f.getInterventionDate(),
                f.getInsuranceDate(),
                remiseClesPrevue(article, tranche),
                f.getDeliveryDateReal(),
                Champs.de(tranche, CfgTranche::getOccupancyPermitsDate),
                f.getPrecisionActDate());
    }

    /** Comme {@code Sale.cs:DeliveryDate} : la date de l'article, sinon le début de finalisation de la tranche. */
    private static LocalDateTime remiseClesPrevue(StkItem article, CfgTranche tranche) {
        LocalDateTime surArticle = Champs.de(article, StkItem::getDeliveryDate);
        return surArticle != null ? surArticle : Champs.de(tranche, CfgTranche::getStartDateFinalisation);
    }

    private static DatesAutresFicheDto datesAutres(ComFolder f) {
        return new DatesAutresFicheDto(
                f.getReservationDateCustomer(),
                f.getReservationRegistryDate(),
                f.getContractPrintDate(),
                f.getAgreementSavingDateCustomer(),
                f.getAgreementSavingDateLawyer(),
                f.getAgreementSavingDateForLegalization(),
                f.getAgreementSavingDateLegalization(),
                f.getAgreementSavingDateForRegistry(),
                f.getDeliveryDateRealCustomer(),
                f.getDeliveryRegistryDate(),
                f.getPrecisionActDateCustomer(),
                f.getPrecisionActDateLawyer(),
                f.getPrecisionActDateForLegalization(),
                f.getPrecisionActDateLegalization(),
                f.getPrecisionActDateForRegistry(),
                f.getPrecisionActRegistryDate(),
                f.getFinalTitleDateDeposit(),
                f.getFinalTitleDateWithdrawal(),
                f.getFinalTitleDate(),
                f.getFinalTitleNumber());
    }

    private static AutresFicheDto autres(ComFolder f, String decisionMeilleurTaux) {
        return new AutresFicheDto(
                Champs.de(f.getCfgSaleManager(), CfgTier::getId),
                TierNomFormatter.nom(f.getCfgSaleManager()),
                Champs.de(f.getCfgSaleManager2Id(), CfgTier::getId),
                TierNomFormatter.nom(f.getCfgSaleManager2Id()),
                Champs.de(f.getCreditBank(), CfgTier::getId),
                TierNomFormatter.nom(f.getCreditBank()),
                f.getCreditAgency(),
                Champs.de(f.getCfgLawyer(), CfgTier::getId),
                TierNomFormatter.nom(f.getCfgLawyer()),
                f.getNotes(),
                f.getIsOldCustomer(),
                f.getIsWithGiftChoiceLater(),
                f.getPriceWithModification(),
                f.getLoanCompanySendDate(),
                decisionMeilleurTaux,
                f.getLoanCompanyDecisionDate(),
                f.getIsPenaltyDisabled(),
                f.getComLoanCompanyDecisionId(),
                Champs.de(f.getCfgCommercial(), CfgTier::getId));
    }
}
