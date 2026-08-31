package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.CategorieMontantDto;
import com.tsix.immocrm.dto.dossier.FicheTropPercuData;
import com.tsix.immocrm.dto.dossier.FicheTropPercuData.Acquereur;
import com.tsix.immocrm.dto.dossier.FicheTropPercuData.LigneReglement;
import com.tsix.immocrm.dto.dossier.FicheTropPercuData.SousArticle;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Gathers the data behind the "Fiche Trop Perçu" document (see {@code FicheTropPercuPdfRenderer}
 * for the actual PDF rendering) — modeled on the legacy Stimulsoft template
 * {@code instructions/templates/ComSaleFicheTropPercu.yes}, a receipt acknowledging that the
 * client's settlements exceed the dossier's price. Reuses {@link DossierService#montants} for the
 * "frais" total (never re-derives raw {@code ComDocument.excludeFromPrice} sums here), and the
 * same acquéreur/co-acquéreur ({@code ComFolderCustomer}) and "société" ({@code CfgProject.cfgCompany})
 * lookups as {@link FicheBancaireService}.
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
        CfgTranche tranche = article != null ? article.getStkHierarchy() : null;
        // Chemin confirmé (même que ActeReservationService/FicheReservationService/FicheBancaireService) :
        // StkItem.CfgProjectId n'est jamais peuplé en données réelles.
        CfgProject projet = tranche != null ? tranche.getCfgProject() : null;

        List<Acquereur> acquereurs = new ArrayList<>();
        if (folder.getCfgCustomer() != null) {
            acquereurs.add(toAcquereur(folder.getCfgCustomer()));
        }
        for (ComFolderCustomer coAcquereur : folderCustomerRepository.findByComFolderId(dossierId)) {
            acquereurs.add(toAcquereur(coAcquereur.getCfgTier()));
        }

        boolean superficieApproximative = article == null
                || article.getTitleNumber() == null || article.getTitleNumber().isBlank();

        List<SousArticle> sousArticles = article == null ? List.of() :
                itemRepository.findByComFolderId(dossierId).stream()
                        .filter(it -> !it.getId().equals(article.getId()))
                        .map(FicheTropPercuService::toSousArticle)
                        .sorted(Comparator.comparing(SousArticle::categorieLibelle,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList();

        List<StlSettlementLine> lignes = settlementLineRepository.findByDossierIdOrderByStlSettlementDocDateAsc(dossierId);
        List<LigneReglement> lignesReglement = new ArrayList<>();
        BigDecimal totalRegle = BigDecimal.ZERO;
        for (StlSettlementLine ligne : lignes) {
            BigDecimal montant = ligne.getAmount() != null ? ligne.getAmount() : BigDecimal.ZERO;
            totalRegle = totalRegle.add(montant);
            boolean credit = ligne.getStlSettlement() != null && Boolean.TRUE.equals(ligne.getStlSettlement().getIsCredit());
            lignesReglement.add(new LigneReglement(
                    ligne.getStlSettlement() != null && ligne.getStlSettlement().getDocDate() != null
                            ? ligne.getStlSettlement().getDocDate().toLocalDate() : null,
                    credit ? montant : montant.negate(),
                    credit,
                    ligne.getStlSettlement() != null ? ligne.getStlSettlement().getDescription() : null
            ));
        }

        BigDecimal prixApresRemise = folder.getPriceAfterDiscount() != null ? folder.getPriceAfterDiscount() : BigDecimal.ZERO;
        BigDecimal resteAPayer = prixApresRemise.subtract(totalRegle);
        BigDecimal montantTropPercu = resteAPayer.abs();

        MontantsDossierDto montants = dossierService.montants(dossierId);
        BigDecimal fraisTotal = montants.totalGeneral().subtract(montants.totalSoumisReglePrixTTC());

        return new FicheTropPercuData(
                folder.getCode(),
                folder.getDocDate() != null ? folder.getDocDate().toLocalDate() : null,
                projet != null && projet.getCfgCompany() != null ? projet.getCfgCompany().getDescription() : null,
                projet != null ? adresse(projet.getAddress(), projet.getAddress2(), projet.getAddress3(), projet.getCity()) : null,
                projet != null ? projet.getSignatureCity() : null,
                acquereurs,
                montantTropPercu,
                article != null && article.getStkItemCategory() != null ? article.getStkItemCategory().getDescription() : null,
                article != null ? article.getCode() : null,
                projet != null ? projet.getDescription() : null,
                article != null && article.getStkItemType() != null ? article.getStkItemType().getDescription() : null,
                article != null ? article.getArea() : null,
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

    private static SousArticle toSousArticle(StkItem item) {
        var categorie = item.getStkItemCategory();
        return new SousArticle(
                categorie != null ? categorie.getDescription() : null,
                categorie != null ? categorie.getCode() : null,
                item.getCode(),
                item.getArea()
        );
    }

    private static Acquereur toAcquereur(CfgTier tier) {
        return new Acquereur(
                tier.getAdmTierTitle() != null ? tier.getAdmTierTitle().getDescription() : null,
                nomTier(tier),
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

    private static String nomTier(CfgTier tier) {
        return com.tsix.immocrm.utils.TierNomFormatter.nom(tier);
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
