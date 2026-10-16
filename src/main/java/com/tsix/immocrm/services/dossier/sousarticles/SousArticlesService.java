package com.tsix.immocrm.services.dossier.sousarticles;

import com.tsix.immocrm.domain.ConventionRemise;
import com.tsix.immocrm.dto.dossier.sousarticles.SousArticleDisponibleDto;
import com.tsix.immocrm.dto.dossier.sousarticles.SousArticleDto;
import com.tsix.immocrm.dto.dossier.sousarticles.SousArticlesDto;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.dossier.DossierSorti;
import com.tsix.immocrm.services.dossier.HistoriqueDossier;
import com.tsix.immocrm.services.dossier.TotauxDossierEcheancier;
import com.tsix.immocrm.services.dossier.fiche.EcritureDossier;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.Requis;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Onglet Sous-articles ({@code Sale.cs} front : {@code AddUnderItem}, {@code DeleteUnderItem}) : ajout depuis les
 * articles libres de la tranche, retrait sauf « affecté par défaut », remise recalculée par {@link RecalculRemise},
 * historique AFFECTATION / ANNULATION AFFECTATION. Voir FEATURE-vente-echeancier.md §2.
 */
@Service
@Transactional(readOnly = true)
public class SousArticlesService {

    static final String AFFECTE_PAR_DEFAUT =
            "Vous ne pouvez pas supprimer un sous article affecté par défaut à l'article en cours !";

    /** {@code UnderItems.cs} : même tranche et vocation, sous-article libre, ni rattaché ni loué ni toujours affecté. */
    private static final String DISPONIBLES = """
            SELECT CAST(i.PKey AS nvarchar(36)) FROM StkItem i
            WHERE i.StkHierarchyId = :tranche AND i.StkVocationId = :vocation AND i.IsUnderItem = 1
              AND i.ParentId IS NULL AND ISNULL(i.AlwaysAffected, 0) = 0
              AND i.ComFolderId IS NULL AND i.RntFolderId IS NULL AND i.PKey <> :lot
            ORDER BY i.Code ASC""";

    private final EcritureDossier ecriture;
    private final ComFolderItemRepository folderItemRepository;
    private final StkItemRepository stkItemRepository;
    private final HistoriqueDossier historique;
    private final EntityManager entityManager;
    private final TotauxDossierEcheancier totaux;

    public SousArticlesService(EcritureDossier ecriture, ComFolderItemRepository folderItemRepository,
                               StkItemRepository stkItemRepository, HistoriqueDossier historique,
                               EntityManager entityManager, TotauxDossierEcheancier totaux) {
        this.ecriture = ecriture;
        this.folderItemRepository = folderItemRepository;
        this.stkItemRepository = stkItemRepository;
        this.historique = historique;
        this.entityManager = entityManager;
        this.totaux = totaux;
    }

    public SousArticlesDto lister(UUID dossierId) {
        return dto(ecriture.charger(dossierId));
    }

    public List<SousArticleDisponibleDto> disponibles(UUID dossierId) {
        ComFolder dossier = ecriture.charger(dossierId);
        return stkItemRepository.findAllById(idsDisponibles(dossier)).stream()
                .sorted(Comparator.comparing(StkItem::getCode, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(SousArticlesService::disponible)
                .toList();
    }

    @Transactional
    public SousArticlesDto ajouter(UUID dossierId, UUID articleId) {
        ComFolder dossier = ecriture.charger(dossierId);
        exigerDossierActif(dossier);
        Requis.champ(articleId, "articleId est requis");
        StkItem stock = stkItemRepository.findById(articleId)
                .orElseThrow(() -> new InvalidRequestException("Article introuvable: " + articleId));
        if (!idsDisponibles(dossier).contains(articleId)) {
            throw new ConflictException("L'article " + stock.getCode() + " n'est pas un sous-article libre de la "
                    + "tranche et de la vocation du dossier.");
        }
        folderItemRepository.save(nouvelleLigne(dossier, stock));
        stock.setComFolderId(dossier.getId());
        stock.setParent(dossier.getStkItem());
        stock.setIsUnderItem(true);
        stkItemRepository.save(stock);
        historique.ligneStatut(dossier, dossier.getStrStatus(), stock, HistoriqueDossier.AFFECTATION);
        return recalculer(dossier);
    }

    @Transactional
    public SousArticlesDto retirer(UUID dossierId, UUID ligneId) {
        ComFolder dossier = ecriture.charger(dossierId);
        exigerDossierActif(dossier);
        ComFolderItem ligne = folderItemRepository.findById(ligneId)
                .filter(l -> l.getComFolder() != null && dossierId.equals(l.getComFolder().getId()))
                .filter(l -> Boolean.TRUE.equals(l.getIsUnderItem()))
                .orElseThrow(() -> new ResourceNotFoundException("Sous-article introuvable pour ce dossier: " + ligneId));
        if (Boolean.TRUE.equals(ligne.getAlwaysAffected())) {
            throw new ConflictException(AFFECTE_PAR_DEFAUT);
        }
        StkItem stock = ligne.getStkItem();
        folderItemRepository.delete(ligne);
        if (stock != null && dossierId.equals(stock.getComFolderId())) {
            stock.setComFolderId(null);
            stock.setParent(null);
            stock.setIsUnderItem(stock.getIsUnderItemOrigin());
            stkItemRepository.save(stock);
        }
        historique.ligneStatut(dossier, dossier.getStrStatus(), stock, HistoriqueDossier.ANNULATION_AFFECTATION);
        folderItemRepository.flush();
        return recalculer(dossier);
    }

    private SousArticlesDto recalculer(ComFolder dossier) {
        List<ComFolderItem> sousArticles = sousArticles(dossier.getId());
        RecalculRemise.appliquer(dossier, sousArticles);
        folderItemRepository.saveAll(sousArticles);
        ecriture.signer(dossier);
        // Le net à payer suit le nouveau prix ; le schéma n'est pas rééquilibré, comme OMRAN.
        totaux.rafraichir(dossier);
        return dto(dossier);
    }

    private List<ComFolderItem> sousArticles(UUID dossierId) {
        return folderItemRepository.findByComFolderId(dossierId).stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsUnderItem()))
                .sorted(Comparator.comparing(ComFolderItem::getCode, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<UUID> idsDisponibles(ComFolder dossier) {
        StkItem lot = dossier.getStkItem();
        if (lot == null || lot.getStkHierarchy() == null || dossier.getStkVocation() == null) {
            return List.of();
        }
        List<?> ids = entityManager.createNativeQuery(DISPONIBLES)
                .setParameter("tranche", lot.getStkHierarchy().getId())
                .setParameter("vocation", dossier.getStkVocation().getId())
                .setParameter("lot", lot.getId())
                .getResultList();
        return ids.stream().map(id -> UUID.fromString(id.toString())).toList();
    }

    /** {@code UnderItem(Yes.Stk.Building.UnderItem, Sale)} : descriptifs et prix HT du stock, prix calculés ensuite. */
    private ComFolderItem nouvelleLigne(ComFolder dossier, StkItem stock) {
        ComFolderItem ligne = new ComFolderItem();
        ligne.setId(UUID.randomUUID());
        ligne.setComFolder(dossier);
        ligne.setStkItem(stock);
        ligne.setParentId(Champs.de(stock.getParent(), StkItem::getId));
        ligne.setCode(stock.getCode());
        ligne.setDescription(stock.getDescription());
        ligne.setAddress(stock.getAddress());
        ligne.setNotes(stock.getNotes());
        ligne.setContenance(stock.getContenance());
        ligne.setArea(stock.getArea());
        ligne.setAreaNet(stock.getArea());
        ligne.setAreaFloor(stock.getAreaFloor());
        ligne.setVATRatio(stock.getVat());
        ligne.setPriceBeforeDiscountHT(stock.getSalePriceHT());
        ligne.setAlwaysAffected(Boolean.TRUE.equals(stock.getAlwaysAffected()));
        ligne.setIsUnderItem(true);
        ligne.setIsUnderItemOrigin(Boolean.TRUE.equals(stock.getIsUnderItemOrigin()));
        ligne.setIsTitled(Boolean.TRUE.equals(stock.getIsTitled()));
        ligne.setStkItemType(stock.getStkItemType());
        ligne.setStkItemCategory(stock.getStkItemCategory());
        ligne.setStkBuildingId(stock.getStkBuilding() != null ? stock.getStkBuilding().getId() : null);
        ligne.setStkFloorId(stock.getStkFloor() != null ? stock.getStkFloor().getId() : null);
        String auteur = ecriture.auteur().nom();
        ligne.setUserCreate(auteur);
        ligne.setUserCreateDateTime(ecriture.maintenant());
        ligne.setUserUpdate(auteur);
        ligne.setUserUpdateDateTime(ecriture.maintenant());
        return ligne;
    }

    /** Un dossier sorti a libéré ses articles : y rattacher un sous-article le réoccuperait. */
    private static void exigerDossierActif(ComFolder dossier) {
        if (DossierSorti.estSorti(dossier)) {
            throw new ConflictException("Dossier annulé ou désisté : ses sous-articles ne se modifient plus.");
        }
    }

    private SousArticlesDto dto(ComFolder f) {
        List<SousArticleDto> lignes = sousArticles(f.getId()).stream().map(SousArticlesService::ligne).toList();
        return new SousArticlesDto(lignes, ConventionRemise.pourcentage(f.getDiscount()), f.getDiscountAmount(),
                f.getPriceBeforeDiscountHT(), f.getPriceBeforeDiscount(), f.getPriceAfterDiscountHT(),
                f.getPriceAfterDiscount());
    }

    private static SousArticleDto ligne(ComFolderItem l) {
        return new SousArticleDto(l.getId(), Champs.de(l.getStkItem(), StkItem::getId), l.getCode(), l.getDescription(),
                Champs.de(l.getStkItemCategory(), StkItemCategory::getCode), Champs.de(l.getStkItemType(), StkItemType::getCode),
                l.getArea(), l.getVATRatio(), l.getPriceBeforeDiscountHT(), l.getPriceBeforeDiscount(),
                ConventionRemise.pourcentage(l.getDiscount()), l.getPriceHT(), l.getPrice(),
                Boolean.TRUE.equals(l.getAlwaysAffected()), Boolean.TRUE.equals(l.getIsUnderItemOrigin()));
    }

    private static SousArticleDisponibleDto disponible(StkItem i) {
        return new SousArticleDisponibleDto(i.getId(), i.getCode(), i.getDescription(),
                Champs.de(i.getStkItemCategory(), StkItemCategory::getCode), Champs.de(i.getStkItemType(), StkItemType::getCode),
                i.getArea(), i.getSalePriceHT(), i.getVat());
    }
}
