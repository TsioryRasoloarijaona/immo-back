package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.CategorieMontantDto;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.dto.dossier.RecuReglementData;
import com.tsix.immocrm.dto.dossier.RecuReglementData.Beneficiaire;
import com.tsix.immocrm.dto.dossier.RecuReglementData.LigneReglement;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * « Reçu de règlement » (page 1 du template {@code RECEIP/Yes_Stl_Front_Credit_Settlement_*.yes}).
 * « Reste à payer » repris de {@link DossierService#montants}, jamais recalculé ici.
 */
@Service
@Transactional(readOnly = true)
public class RecuReglementService {

    private static final String CATEGORIE_FRAIS_DOSSIER = "FRAIS DOSSIER";
    private static final String CATEGORIE_FRAIS_SYNDIC = "FRAIS SYNDIC";

    private final ComFolderRepository folderRepository;
    private final ComFolderCustomerRepository folderCustomerRepository;
    private final StlSettlementRepository settlementRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final DossierService dossierService;

    public RecuReglementService(ComFolderRepository folderRepository, ComFolderCustomerRepository folderCustomerRepository,
                                 StlSettlementRepository settlementRepository, StlSettlementLineRepository settlementLineRepository,
                                 DossierService dossierService) {
        this.folderRepository = folderRepository;
        this.folderCustomerRepository = folderCustomerRepository;
        this.settlementRepository = settlementRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.dossierService = dossierService;
    }

    public RecuReglementData collect(UUID dossierId, UUID reglementId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));
        StlSettlement settlement = settlementRepository.findById(reglementId)
                .orElseThrow(() -> new ResourceNotFoundException("Règlement introuvable: " + reglementId));

        List<StlSettlementLine> lignesDeCeReglement = settlementLineRepository.findBySettlementIdAndDossierId(reglementId, dossierId);
        if (lignesDeCeReglement.isEmpty()) {
            throw new ResourceNotFoundException("Ce règlement n'est pas rattaché à ce dossier: " + reglementId);
        }
        BigDecimal montantRegleDossier = lignesDeCeReglement.stream()
                .map(StlSettlementLine::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StkItem article = folder.getStkItem();
        CfgTranche tranche = Champs.de(article, StkItem::getStkHierarchy);
        // Projet via la tranche : StkItem.CfgProjectId n'est jamais peuplé.
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);

        CfgTier beneficiaireTier = settlement.getTier() != null ? settlement.getTier() : folder.getCfgCustomer();
        Beneficiaire beneficiaire = beneficiaireTier != null ? toBeneficiaire(beneficiaireTier) : null;

        List<Beneficiaire> coAcquereurs = coAcquereurs(dossierId, beneficiaireTier);

        boolean superficieApproximative = article == null
                || article.getTitleNumber() == null || article.getTitleNumber().isBlank();

        List<StlSettlementLine> toutesLesLignes = settlementLineRepository.findByDossierIdOrderByStlSettlementDocDateAsc(dossierId);
        List<LigneReglement> lignesReglement = new ArrayList<>();
        for (StlSettlementLine ligne : toutesLesLignes) {
            lignesReglement.add(toLigneReglement(ligne));
        }

        MontantsDossierDto montants = dossierService.montants(dossierId);
        BigDecimal resteAPayer = BigDecimal.ZERO;
        for (CategorieMontantDto ligne : montants.parCategorie()) {
            if (CATEGORIE_FRAIS_DOSSIER.equals(ligne.categorie()) || CATEGORIE_FRAIS_SYNDIC.equals(ligne.categorie())) {
                continue;
            }
            resteAPayer = resteAPayer.add(ligne.reste() != null ? ligne.reste() : BigDecimal.ZERO);
        }

        CfgCompany entreprise = settlement.getCfgCompany();

        return new RecuReglementData(
                folder.getCode(),
                settlement.getReceiptCode(),
                Boolean.TRUE.equals(settlement.getIsCredit()),
                montantRegleDossier,
                settlement.getDescription(),
                beneficiaire,
                coAcquereurs,
                Champs.de(entreprise, CfgCompany::getDescription),
                Champs.de(entreprise, CfgCompany::getLegalForm),
                entreprise != null ? adresse(entreprise.getAddress(), entreprise.getAddress2(), entreprise.getAddress3(), entreprise.getCity()) : null,
                Champs.de(entreprise, CfgCompany::getSignatureCity),
                folder.getDocDate() != null ? folder.getDocDate().toLocalDate() : null,
                Champs.de(article, StkItem::getStkItemCategory, StkItemCategory::getDescription),
                Champs.de(article, StkItem::getCode),
                Champs.de(projet, CfgProject::getDescription),
                Champs.de(article, StkItem::getStkItemType, StkItemType::getDescription),
                Champs.de(article, StkItem::getArea),
                superficieApproximative,
                folder.getPriceAfterDiscount(),
                lignesReglement,
                resteAPayer
        );
    }

    /** Co-acquéreurs du dossier, sans le bénéficiaire du reçu. */
    private List<Beneficiaire> coAcquereurs(UUID dossierId, CfgTier beneficiaireTier) {
        List<Beneficiaire> coAcquereurs = new ArrayList<>();
        for (ComFolderCustomer coAcquereur : folderCustomerRepository.findByComFolderId(dossierId)) {
            CfgTier tier = coAcquereur.getCfgTier();
            if (tier != null && (beneficiaireTier == null || !tier.getId().equals(beneficiaireTier.getId()))) {
                coAcquereurs.add(toBeneficiaire(tier));
            }
        }
        return coAcquereurs;
    }

    private static LigneReglement toLigneReglement(StlSettlementLine ligne) {
        BigDecimal montant = ligne.getAmount() != null ? ligne.getAmount() : BigDecimal.ZERO;
        StlSettlement reglement = ligne.getStlSettlement();
        boolean credit = reglement != null && Boolean.TRUE.equals(reglement.getIsCredit());
        return new LigneReglement(
                reglement != null && reglement.getDocDate() != null ? reglement.getDocDate().toLocalDate() : null,
                credit ? montant : montant.negate(),
                credit,
                Champs.de(reglement, StlSettlement::getDescription)
        );
    }

    private static Beneficiaire toBeneficiaire(CfgTier tier) {
        return new Beneficiaire(
                tier.getAdmTierTitle() != null ? tier.getAdmTierTitle().getDescription() : null,
                TierNomFormatter.nom(tier),
                pieceIdentite(tier)
        );
    }

    private static String pieceIdentite(CfgTier tier) {
        String type = tier.getAdmIdentityDocumentType() != null ? tier.getAdmIdentityDocumentType().getCode() : null;
        if (type == null && tier.getIDNumber() == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (type != null) {
            sb.append(type);
        }
        if (tier.getIDNumber() != null) {
            if (!sb.isEmpty()) sb.append(" N° ");
            sb.append(tier.getIDNumber());
        }
        return sb.toString();
    }

    private static String adresse(String a1, String a2, String a3, String ville) {
        StringBuilder sb = new StringBuilder();
        for (String part : new String[]{a1, a2, a3, ville}) {
            if (part != null && !part.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(part.trim());
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
