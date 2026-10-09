package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.dto.dossier.CategorieMontantDto;
import com.tsix.immocrm.dto.dossier.FicheTropPercuData;
import com.tsix.immocrm.dto.dossier.FicheTropPercuData.Acquereur;
import com.tsix.immocrm.dto.dossier.FicheTropPercuData.LigneReglement;
import com.tsix.immocrm.dto.dossier.FicheTropPercuData.SousArticle;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * « Fiche Trop Perçu » (template {@code ComSaleFicheTropPercu.yes}) : les règlements dépassent le prix.
 * Total des frais repris de {@link DossierService#montants}, jamais recalculé ici.
 */
@Service
@Transactional(readOnly = true)
public class FicheTropPercuService {

    private final ComFolderRepository folderRepository;
    private final ComFolderCustomerRepository folderCustomerRepository;
    private final StkItemRepository itemRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final DossierService dossierService;

    public FicheTropPercuService(ComFolderRepository folderRepository, ComFolderCustomerRepository folderCustomerRepository,
                                  StkItemRepository itemRepository, StlSettlementLineRepository settlementLineRepository,
                                  DossierService dossierService) {
        this.folderRepository = folderRepository;
        this.folderCustomerRepository = folderCustomerRepository;
        this.itemRepository = itemRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.dossierService = dossierService;
    }

    public FicheTropPercuData collect(UUID dossierId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));

        StkItem article = folder.getStkItem();
        CfgTranche tranche = Champs.de(article, StkItem::getStkHierarchy);
        // Projet via la tranche : StkItem.CfgProjectId n'est jamais peuplé.
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);

        List<Acquereur> acquereurs = new ArrayList<>();
        if (folder.getCfgCustomer() != null) {
            acquereurs.add(toAcquereur(folder.getCfgCustomer()));
        }
        folderCustomerRepository.findByComFolderId(dossierId).stream()
                .map(coAcquereur -> toAcquereur(coAcquereur.getCfgTier()))
                .forEach(acquereurs::add);

        boolean superficieApproximative = article == null
                || article.getTitleNumber() == null || article.getTitleNumber().isBlank();

        List<SousArticle> sousArticles = article == null ? List.of() :
                itemRepository.findByComFolderId(dossierId).stream()
                        .filter(it -> !it.getId().equals(article.getId()))
                        .map(FicheTropPercuService::toSousArticle)
                        .sorted(Comparator.comparing(SousArticle::categorieLibelle,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList();

        List<StlSettlementLine> lignes = settlementLineRepository.findByDossierIdOrderByStlSettlementDocDateAsc(dossierId)
                .stream().filter(StatutReglement::compteDansLesTotaux).toList();
        List<LigneReglement> lignesReglement = new ArrayList<>();
        BigDecimal totalRegle = BigDecimal.ZERO;
        for (StlSettlementLine ligne : lignes) {
            BigDecimal montant = ligne.getAmount() != null ? ligne.getAmount() : BigDecimal.ZERO;
            totalRegle = totalRegle.add(montant);
            lignesReglement.add(toLigneReglement(ligne, montant));
        }

        BigDecimal prixApresRemise = folder.getPriceAfterDiscount() != null ? folder.getPriceAfterDiscount() : BigDecimal.ZERO;
        BigDecimal resteAPayer = prixApresRemise.subtract(totalRegle);
        BigDecimal montantTropPercu = resteAPayer.abs();

        MontantsDossierDto montants = dossierService.montants(dossierId);
        BigDecimal fraisTotal = montants.totalGeneral().subtract(montants.totalSoumisReglePrixTTC());

        return new FicheTropPercuData(
                folder.getCode(),
                folder.getDocDate() != null ? folder.getDocDate().toLocalDate() : null,
                Champs.de(projet, CfgProject::getCfgCompany, CfgCompany::getDescription),
                projet != null ? adresse(projet.getAddress(), projet.getAddress2(), projet.getAddress3(), projet.getCity()) : null,
                Champs.de(projet, CfgProject::getSignatureCity),
                acquereurs,
                montantTropPercu,
                Champs.de(article, StkItem::getStkItemCategory, StkItemCategory::getDescription),
                Champs.de(article, StkItem::getCode),
                Champs.de(projet, CfgProject::getDescription),
                Champs.de(article, StkItem::getStkItemType, StkItemType::getDescription),
                Champs.de(article, StkItem::getArea),
                superficieApproximative,
                sousArticles,
                prixApresRemise,
                folder.getUnitCost(),
                fraisTotal,
                lignesReglement,
                totalRegle,
                resteAPayer
        );
    }

    private static LigneReglement toLigneReglement(StlSettlementLine ligne, BigDecimal montant) {
        StlSettlement reglement = ligne.getStlSettlement();
        boolean credit = reglement != null && Boolean.TRUE.equals(reglement.getIsCredit());
        return new LigneReglement(
                reglement != null && reglement.getDocDate() != null ? reglement.getDocDate().toLocalDate() : null,
                credit ? montant : montant.negate(),
                credit,
                Champs.de(reglement, StlSettlement::getDescription)
        );
    }

    private static SousArticle toSousArticle(StkItem item) {
        StkItemCategory categorie = item.getStkItemCategory();
        return new SousArticle(
                Champs.de(categorie, StkItemCategory::getDescription),
                Champs.de(categorie, StkItemCategory::getCode),
                item.getCode(),
                item.getArea()
        );
    }

    private static Acquereur toAcquereur(CfgTier tier) {
        return new Acquereur(
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
