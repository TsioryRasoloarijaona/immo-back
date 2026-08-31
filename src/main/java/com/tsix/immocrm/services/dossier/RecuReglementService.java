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
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gathers the data behind the "Reçu de règlement" document (see {@code RecuReglementPdfRenderer}
 * for the actual PDF rendering) — modeled on "Page1" of the legacy Stimulsoft template
 * {@code instructions/templates/RECEIP/Yes_Stl_Front_Credit_Settlement_20240301102347.yes}, an
 * "ATTESTATION DE RÉCEPTION CHEQUE" acknowledging one specific règlement. Reuses
 * {@link DossierService#montants} for the "Reste à payer" (never re-derives raw
 * {@code ComSaleFolderView.PriceRest}-style sums here), and the same acquéreur/co-acquéreur
 * ({@code ComFolderCustomer}) lookup and "situation des chèques reçus" listing as
 * {@code FicheTropPercuService}.
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
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StkItem article = folder.getStkItem();
        CfgTranche tranche = article != null ? article.getStkHierarchy() : null;
        // Chemin confirmé (même que FicheTropPercuService/FicheBancaireService) : StkItem.CfgProjectId
        // n'est jamais peuplé en données réelles.
        CfgProject projet = tranche != null ? tranche.getCfgProject() : null;

        CfgTier beneficiaireTier = settlement.getTier() != null ? settlement.getTier() : folder.getCfgCustomer();
        Beneficiaire beneficiaire = beneficiaireTier != null ? toBeneficiaire(beneficiaireTier) : null;

        List<Beneficiaire> coAcquereurs = new ArrayList<>();
        for (ComFolderCustomer coAcquereur : folderCustomerRepository.findByComFolderId(dossierId)) {
            CfgTier tier = coAcquereur.getCfgTier();
            if (tier != null && (beneficiaireTier == null || !tier.getId().equals(beneficiaireTier.getId()))) {
                coAcquereurs.add(toBeneficiaire(tier));
            }
        }

        boolean superficieApproximative = article == null
                || article.getTitleNumber() == null || article.getTitleNumber().isBlank();

        List<StlSettlementLine> toutesLesLignes = settlementLineRepository.findByDossierIdOrderByStlSettlementDocDateAsc(dossierId);
        List<LigneReglement> lignesReglement = new ArrayList<>();
        for (StlSettlementLine ligne : toutesLesLignes) {
            BigDecimal montant = ligne.getAmount() != null ? ligne.getAmount() : BigDecimal.ZERO;
            boolean credit = ligne.getStlSettlement() != null && Boolean.TRUE.equals(ligne.getStlSettlement().getIsCredit());
            lignesReglement.add(new LigneReglement(
                    ligne.getStlSettlement() != null && ligne.getStlSettlement().getDocDate() != null
                            ? ligne.getStlSettlement().getDocDate().toLocalDate() : null,
                    credit ? montant : montant.negate(),
                    credit,
                    ligne.getStlSettlement() != null ? ligne.getStlSettlement().getDescription() : null
            ));
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
                entreprise != null ? entreprise.getDescription() : null,
                entreprise != null ? entreprise.getLegalForm() : null,
                entreprise != null ? adresse(entreprise.getAddress(), entreprise.getAddress2(), entreprise.getAddress3(), entreprise.getCity()) : null,
                entreprise != null ? entreprise.getSignatureCity() : null,
                folder.getDocDate() != null ? folder.getDocDate().toLocalDate() : null,
                article != null && article.getStkItemCategory() != null ? article.getStkItemCategory().getDescription() : null,
                article != null ? article.getCode() : null,
                projet != null ? projet.getDescription() : null,
                article != null && article.getStkItemType() != null ? article.getStkItemType().getDescription() : null,
                article != null ? article.getArea() : null,
                superficieApproximative,
                folder.getPriceAfterDiscount(),
                lignesReglement,
                resteAPayer
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
