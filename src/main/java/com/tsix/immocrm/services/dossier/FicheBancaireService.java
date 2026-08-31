package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.CategorieMontantDto;
import com.tsix.immocrm.dto.dossier.FicheBancaireData;
import com.tsix.immocrm.dto.dossier.FicheBancaireData.Acquereur;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gathers the data behind the "Fiche Bancaire" document (see {@code FicheBancairePdfRenderer} for
 * the actual PDF rendering) — modeled on the legacy "FICHE BANCAIRE" template, a mortgage-support
 * sheet. Reuses {@link DossierService#montants} (the same category recap
 * {@code GET /api/dossiers/{id}/montants} exposes) for the "FRAIS DOSSIER"/"FRAIS SYNDIC"
 * réglé/reste split — never re-derives raw settlement sums here.
 */
@Service
@Transactional(readOnly = true)
public class FicheBancaireService {

    private static final String CATEGORIE_FRAIS_DOSSIER = "FRAIS DOSSIER";
    private static final String CATEGORIE_FRAIS_SYNDIC = "FRAIS SYNDIC";
    private static final BigDecimal DIX_MILLE = BigDecimal.valueOf(10_000);

    private final ComFolderRepository folderRepository;
    private final ComFolderCustomerRepository folderCustomerRepository;
    private final StkItemRepository itemRepository;
    private final DossierService dossierService;

    public FicheBancaireService(ComFolderRepository folderRepository, ComFolderCustomerRepository folderCustomerRepository,
                                 StkItemRepository itemRepository, DossierService dossierService) {
        this.folderRepository = folderRepository;
        this.folderCustomerRepository = folderCustomerRepository;
        this.itemRepository = itemRepository;
        this.dossierService = dossierService;
    }

    public FicheBancaireData collect(UUID dossierId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));

        StkItem article = folder.getStkItem();
        CfgTranche tranche = article != null ? article.getStkHierarchy() : null;
        // Confirmed path (same as ActeReservationService/FicheReservationService):
        // StkItem.CfgProjectId is never populated in real data.
        CfgProject projet = tranche != null ? tranche.getCfgProject() : null;

        List<Acquereur> acquereurs = new ArrayList<>();
        if (folder.getCfgCustomer() != null) {
            acquereurs.add(toAcquereur(folder.getCfgCustomer(), folder.getPartPercent()));
        }
        for (ComFolderCustomer coAcquereur : folderCustomerRepository.findByComFolderId(dossierId)) {
            acquereurs.add(toAcquereur(coAcquereur.getCfgTier(), coAcquereur.getPartPercent()));
        }

        BigDecimal superficieSousArticles = article == null ? BigDecimal.ZERO :
                itemRepository.findByComFolderId(dossierId).stream()
                        .filter(it -> !it.getId().equals(article.getId()))
                        .map(StkItem::getArea)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        MontantsDossierDto montants = dossierService.montants(dossierId);
        CategorieMontantDto fraisDossier = trouverCategorie(montants, CATEGORIE_FRAIS_DOSSIER);
        CategorieMontantDto fraisSyndic = trouverCategorie(montants, CATEGORIE_FRAIS_SYNDIC);

        BigDecimal montantVerse = BigDecimal.ZERO;
        BigDecimal reliquat = BigDecimal.ZERO;
        for (CategorieMontantDto ligne : montants.parCategorie()) {
            if (CATEGORIE_FRAIS_DOSSIER.equals(ligne.categorie()) || CATEGORIE_FRAIS_SYNDIC.equals(ligne.categorie())) {
                continue;
            }
            montantVerse = montantVerse.add(ligne.regle() != null ? ligne.regle() : BigDecimal.ZERO);
            reliquat = reliquat.add(ligne.reste() != null ? ligne.reste() : BigDecimal.ZERO);
        }

        return new FicheBancaireData(
                folder.getCode(),
                folder.getBankRecordDate() != null ? folder.getBankRecordDate().toLocalDate() : null,
                projet != null && projet.getCfgCompany() != null ? projet.getCfgCompany().getDescription() : null,
                projet != null ? projet.getDescription() : null,
                projet != null ? adresse(projet.getAddress(), projet.getAddress2(), projet.getAddress3(), projet.getCity()) : null,
                projet != null ? projet.getSignatureCity() : null,
                acquereurs,
                article != null && article.getStkItemCategory() != null ? article.getStkItemCategory().getDescription() : null,
                article != null && article.getStkBuilding() != null ? article.getStkBuilding().getDescription() : null,
                article != null && article.getStkFloor() != null ? article.getStkFloor().getDescription() : null,
                article != null ? article.getCode() : null,
                article != null ? article.getFractionDivided() : null,
                article != null ? article.getPropertyCalled() : null,
                article != null ? article.getContenance() : null,
                article != null ? article.getArea() : null,
                article != null ? article.getTitleNumberMother() : null,
                article != null ? article.getTitleNumber() : null,
                article != null ? diviserSur10000(article.getDividedPart()) : null,
                article != null ? diviserSur10000(article.getUndividedPart()) : null,
                article != null ? article.getFractionDividedLand() : null,
                superficieSousArticles,
                folder.getPriceAfterDiscount(),
                montantVerse,
                reliquat,
                fraisDossier != null ? fraisDossier.montant() : null,
                fraisDossier != null ? fraisDossier.regle() : null,
                fraisDossier != null ? fraisDossier.reste() : null,
                fraisSyndic != null ? fraisSyndic.montant() : null,
                fraisSyndic != null ? fraisSyndic.regle() : null,
                fraisSyndic != null ? fraisSyndic.reste() : null
        );
    }

    private static CategorieMontantDto trouverCategorie(MontantsDossierDto montants, String categorie) {
        return montants.parCategorie().stream()
                .filter(c -> categorie.equals(c.categorie()))
                .findFirst()
                .orElse(null);
    }

    /** {@code StkItem.DividedPart}/{@code UndividedPart} sont exprimées "sur 10000" côté legacy. */
    private static BigDecimal diviserSur10000(BigDecimal valeur) {
        return valeur == null ? null : valeur.divide(DIX_MILLE, 6, RoundingMode.HALF_UP);
    }

    private static Acquereur toAcquereur(CfgTier tier, BigDecimal partPercent) {
        return new Acquereur(
                nomTier(tier),
                tier.getAdmProfession() != null ? tier.getAdmProfession().getDescription() : null,
                adresse(tier.getAddress(), tier.getAddress2(), tier.getAddress3(), tier.getCity()),
                tier.getBirthDate() != null ? tier.getBirthDate().toLocalDate() : null,
                tier.getBirthPlace(),
                telephone(tier),
                pieceIdentite(tier),
                partPercent,
                tier.getAdmFamilySituation() != null ? tier.getAdmFamilySituation().getDescription() : null
        );
    }

    private static String telephone(CfgTier tier) {
        String indicatif = tier.getPhoneIndicatif() != null ? tier.getPhoneIndicatif() : "";
        String phone = tier.getPhone() != null ? tier.getPhone() : "";
        String full = (indicatif + " " + phone).trim();
        return full.isEmpty() ? null : full;
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
