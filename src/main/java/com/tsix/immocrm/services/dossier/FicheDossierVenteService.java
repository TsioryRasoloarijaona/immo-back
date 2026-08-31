package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.FicheDossierVenteData;
import com.tsix.immocrm.dto.dossier.FicheDossierVenteData.Acquereur;
import com.tsix.immocrm.dto.dossier.FicheDossierVenteData.DetailReglement;
import com.tsix.immocrm.dto.dossier.FicheDossierVenteData.LigneEcheance;
import com.tsix.immocrm.dto.dossier.FicheDossierVenteData.MontantsFiche;
import com.tsix.immocrm.dto.dossier.FicheDossierVenteData.SousArticleDossier;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gathers the data behind the "Fiche Dossier de Vente" document (see
 * {@code FicheDossierVentePdfRenderer} for the actual PDF rendering) — modeled on the legacy
 * "FICHE COMPLETE" template, a comprehensive internal dossier summary. Reuses
 * {@link DossierHistoriqueService#findHistorique} for the HISTORIQUE table, but computes its own
 * MONTANTS breakdown (see {@link FicheDossierVenteData.MontantsFiche}) rather than
 * {@link DossierService#montants}, since the legacy template's 9 fixed rows don't match this
 * app's free-text-category dynamic grouping.
 */
@Service
@Transactional(readOnly = true)
public class FicheDossierVenteService {

    private static final int NOMBRE_EVENEMENTS_HISTORIQUE = 30;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ComFolderRepository folderRepository;
    private final ComFolderCustomerRepository folderCustomerRepository;
    private final ComFolderItemRepository folderItemRepository;
    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final DossierHistoriqueService historiqueService;

    public FicheDossierVenteService(ComFolderRepository folderRepository, ComFolderCustomerRepository folderCustomerRepository,
                                     ComFolderItemRepository folderItemRepository, ComDocumentRepository documentRepository,
                                     StlSettlementLineRepository settlementLineRepository,
                                     DossierHistoriqueService historiqueService) {
        this.folderRepository = folderRepository;
        this.folderCustomerRepository = folderCustomerRepository;
        this.folderItemRepository = folderItemRepository;
        this.documentRepository = documentRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.historiqueService = historiqueService;
    }

    public FicheDossierVenteData collect(UUID dossierId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));

        StkItem article = folder.getStkItem();
        CfgTranche tranche = article != null ? article.getStkHierarchy() : null;
        CfgProject projet = tranche != null ? tranche.getCfgProject() : null;

        List<Acquereur> acquereurs = new ArrayList<>();
        if (folder.getCfgCustomer() != null) {
            acquereurs.add(new Acquereur(nomTier(folder.getCfgCustomer()), folder.getPartPercent()));
        }
        for (ComFolderCustomer coAcquereur : folderCustomerRepository.findByComFolderId(dossierId)) {
            acquereurs.add(new Acquereur(nomTier(coAcquereur.getCfgTier()), coAcquereur.getPartPercent()));
        }

        List<ComFolderItem> folderItems = folderItemRepository.findByComFolderId(dossierId);
        List<SousArticleDossier> sousArticles = folderItems.stream()
                .filter(it -> Boolean.TRUE.equals(it.getIsUnderItem()))
                .map(it -> new SousArticleDossier(
                        it.getCode(),
                        it.getStkItemCategory() != null ? it.getStkItemCategory().getDescription() : null,
                        it.getArea(),
                        it.getPrice(),
                        it.getDiscount(),
                        it.getPrice(),
                        it.getAlwaysAffected()))
                .toList();

        // Règlements de tout le dossier, groupés par échéance — évite une requête par échéance.
        Map<UUID, List<StlSettlementLine>> reglementsParEcheance = new LinkedHashMap<>();
        for (StlSettlementLine ligne : settlementLineRepository.findByDossierIdOrderByEcheanceDateAsc(dossierId)) {
            reglementsParEcheance.computeIfAbsent(ligne.getComDocument().getId(), k -> new ArrayList<>()).add(ligne);
        }

        List<ComDocument> echeances = documentRepository.findByComFolderIdOrderByDocDateAsc(dossierId);
        List<LigneEcheance> echeancier = echeances.stream()
                .map(d -> toLigneEcheance(d, reglementsParEcheance.getOrDefault(d.getId(), List.of())))
                .toList();

        MontantsFiche montants = montants(folder, echeances, reglementsParEcheance);
        var historique = historiqueService.findHistorique(dossierId, PageRequest.of(0, NOMBRE_EVENEMENTS_HISTORIQUE)).getContent();

        return new FicheDossierVenteData(
                folder.getCode(),
                folder.getStrStatus() != null ? folder.getStrStatus().getDescription() : null,
                article != null && article.getStkItemCategory() != null ? article.getStkItemCategory().getDescription() : null,
                article != null && article.getStkItemType() != null ? article.getStkItemType().getDescription() : null,
                article != null ? article.getCode() : null,
                article != null ? article.getArea() : null,
                nomTier(folder.getCfgCustomer()),
                nomTier(folder.getCfgCommercial()),
                projet != null && projet.getCfgCompany() != null ? projet.getCfgCompany().getDescription() : null,
                projet != null ? projet.getDescription() : null,
                tranche != null ? tranche.getDescription() : null,
                article != null ? article.getTitleNumber() : null,
                folder.getComProspection() != null ? folder.getComProspection().getCode() : null,
                folder.getDocDate(),
                folder.getPartPercent(),
                folder.getStatusValidityDate(),

                folder.getUnitCost(),
                folder.getItemPrice(),
                folder.getUnderItemsPrice(),
                folder.getPriceBeforeDiscount(),
                folder.getDiscount(),
                folder.getDiscountAmount(),
                folder.getPriceAfterDiscount(),
                folder.getAgreementPrice(),

                montants,

                folder.getReservationDate(),
                folder.getAgreementDate(),
                folder.getAgreementRegistryDate(),
                null, // Visite technique: no dedicated ComFolder column confirmed distinct from ReceptionDate
                folder.getReceptionDate(),
                folder.getBankRecordDate(),
                folder.getCreditApprovalDate(),
                folder.getCreditPossessionDate(),
                folder.getInterventionDate(),
                folder.getInsuranceDate(),
                folder.getOwnershipCertificateDate(),
                tranche != null ? tranche.getDeliveryDate() : null,
                folder.getDeliveryDateReal(),
                folder.getCancelDate(),
                folder.getCancelDescription(),

                folder.getVATRatio(),
                folder.getCreditBank() != null || (folder.getCreditAgency() != null && !folder.getCreditAgency().isBlank()),
                nomTier(folder.getCfgLawyer()),
                nomTier(folder.getCreditBank()),
                folder.getCreditAgency(),
                folder.getIsOldCustomer(),
                folder.getIsWithGiftChoiceLater(),
                folder.getPriceWithModification(),
                folder.getLoanCompanySendDate(),
                folder.getLoanCompanyDecisionDate(),

                echeancier,
                sousArticles,
                acquereurs,
                historique,
                folder.getNotes()
        );
    }

    /**
     * Recomputes the MONTANTS box's 9 fixed rows. See {@link MontantsFiche}'s javadoc for the
     * reasoning behind each row's source.
     */
    private static MontantsFiche montants(ComFolder folder, List<ComDocument> echeances,
                                           Map<UUID, List<StlSettlementLine>> reglementsParEcheance) {
        BigDecimal[] frais = sommeParFlag(echeances, reglementsParEcheance, ComSettlementCategory::getIsSaleExpense);
        BigDecimal[] modification = sommeParFlag(echeances, reglementsParEcheance, ComSettlementCategory::getIsSaleModification);
        BigDecimal[] avance = sommeParFlag(echeances, reglementsParEcheance, ComSettlementCategory::getIsSaleAdvance);
        BigDecimal[] credit = sommeParFlag(echeances, reglementsParEcheance, ComSettlementCategory::getIsSaleCredit);
        BigDecimal[] autoFinancement = sommeParFlag(echeances, reglementsParEcheance, ComSettlementCategory::getIsSaleAutoFinancment);
        BigDecimal[] penalite = sommeParFlag(echeances, reglementsParEcheance, ComSettlementCategory::getIsSalePenality);
        BigDecimal[] resiliation = sommeParFlag(echeances, reglementsParEcheance, ComSettlementCategory::getIsSaleResiliation);

        BigDecimal montantPrixVente = zeroSiNull(folder.getPriceAfterDiscount());
        BigDecimal reglePrixVente = avance[1].add(credit[1]).add(autoFinancement[1]);
        BigDecimal ratioAvance = montantPrixVente.signum() != 0
                ? avance[0].divide(montantPrixVente, 4, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal montantNetAPayer = frais[0].add(modification[0]).add(montantPrixVente)
                .add(penalite[0]).add(resiliation[0]);
        BigDecimal regleNetAPayer = frais[1].add(modification[1]).add(reglePrixVente).add(penalite[1]).add(resiliation[1]);

        return new MontantsFiche(
                frais[0], frais[1],
                modification[0], modification[1],
                montantPrixVente, reglePrixVente,
                ratioAvance, avance[0], avance[1],
                credit[0], credit[1],
                autoFinancement[0], autoFinancement[1],
                penalite[0], penalite[1],
                resiliation[0], resiliation[1],
                montantNetAPayer, regleNetAPayer
        );
    }

    /** [montant total, réglé total] des échéances dont la catégorie a le flag booléen donné à vrai. */
    private static BigDecimal[] sommeParFlag(List<ComDocument> echeances, Map<UUID, List<StlSettlementLine>> reglementsParEcheance,
                                              java.util.function.Function<ComSettlementCategory, Boolean> flag) {
        BigDecimal montant = BigDecimal.ZERO;
        BigDecimal regle = BigDecimal.ZERO;
        for (ComDocument d : echeances) {
            ComSettlementCategory categorie = d.getComSettlementCategory();
            if (categorie == null || !Boolean.TRUE.equals(flag.apply(categorie))) {
                continue;
            }
            montant = montant.add(zeroSiNull(d.getAmount()));
            for (StlSettlementLine ligne : reglementsParEcheance.getOrDefault(d.getId(), List.of())) {
                regle = regle.add(zeroSiNull(ligne.getAmount()));
            }
        }
        return new BigDecimal[]{montant, regle};
    }

    private static BigDecimal zeroSiNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static LigneEcheance toLigneEcheance(ComDocument d, List<StlSettlementLine> reglements) {
        BigDecimal regle = reglements.stream().map(StlSettlementLine::getAmount)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montant = zeroSiNull(d.getAmount());
        List<DetailReglement> details = reglements.stream().map(FicheDossierVenteService::toDetailReglement).toList();
        return new LigneEcheance(
                d.getDocDate(),
                d.getComSettlementCategory() != null ? d.getComSettlementCategory().getCode() : null,
                d.getAmount(),
                regle,
                montant.subtract(regle),
                d.getNotes(),
                details
        );
    }

    /** "{description du règlement} N° {code} du {date}" — reproduit le libellé du template legacy. */
    private static DetailReglement toDetailReglement(StlSettlementLine ligne) {
        StlSettlement s = ligne.getStlSettlement();
        StringBuilder libelle = new StringBuilder();
        if (s != null) {
            if (s.getDescription() != null && !s.getDescription().isBlank()) {
                libelle.append(s.getDescription());
            }
            if (s.getCode() != null && !s.getCode().isBlank()) {
                if (!libelle.isEmpty()) libelle.append(' ');
                libelle.append("N° ").append(s.getCode());
            }
            if (s.getSettlementDate() != null) {
                if (!libelle.isEmpty()) libelle.append(' ');
                libelle.append("du ").append(s.getSettlementDate().format(DATE));
            }
        }
        return new DetailReglement(libelle.toString(), ligne.getAmount());
    }

    private static String nomTier(CfgTier tier) {
        return com.tsix.immocrm.utils.TierNomFormatter.nom(tier);
    }
}
