package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.CategorieMontantDto;
import com.tsix.immocrm.dto.dossier.FicheBancaireData;
import com.tsix.immocrm.dto.dossier.FicheBancaireData.Acquereur;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** « Fiche Bancaire » ; réglé/reste des frais repris de {@link DossierService#montants}, jamais recalculés ici. */
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
        CfgTranche tranche = Champs.de(article, StkItem::getStkHierarchy);
        // Projet via la tranche : StkItem.CfgProjectId n'est jamais peuplé.
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);

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
                        .filter(Objects::nonNull)
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
                Champs.de(projet, CfgProject::getCfgCompany, CfgCompany::getDescription),
                Champs.de(projet, CfgProject::getDescription),
                projet != null ? adresse(projet.getAddress(), projet.getAddress2(), projet.getAddress3(), projet.getCity()) : null,
                Champs.de(projet, CfgProject::getSignatureCity),
                acquereurs,
                Champs.de(article, StkItem::getStkItemCategory, StkItemCategory::getDescription),
                Champs.de(article, StkItem::getStkBuilding, StkBuilding::getDescription),
                Champs.de(article, StkItem::getStkFloor, StkFloor::getDescription),
                Champs.de(article, StkItem::getCode),
                Champs.de(article, StkItem::getFractionDivided),
                Champs.de(article, StkItem::getPropertyCalled),
                Champs.de(article, StkItem::getContenance),
                Champs.de(article, StkItem::getArea),
                Champs.de(article, StkItem::getTitleNumberMother),
                Champs.de(article, StkItem::getTitleNumber),
                article != null ? diviserSur10000(article.getDividedPart()) : null,
                article != null ? diviserSur10000(article.getUndividedPart()) : null,
                Champs.de(article, StkItem::getFractionDividedLand),
                superficieSousArticles,
                folder.getPriceAfterDiscount(),
                montantVerse,
                reliquat,
                Champs.de(fraisDossier, CategorieMontantDto::montant),
                Champs.de(fraisDossier, CategorieMontantDto::regle),
                Champs.de(fraisDossier, CategorieMontantDto::reste),
                Champs.de(fraisSyndic, CategorieMontantDto::montant),
                Champs.de(fraisSyndic, CategorieMontantDto::regle),
                Champs.de(fraisSyndic, CategorieMontantDto::reste)
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
                TierNomFormatter.nom(tier),
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
