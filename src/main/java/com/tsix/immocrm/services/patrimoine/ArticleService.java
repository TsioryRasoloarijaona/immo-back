package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.PrixArticle;
import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.dto.patrimoine.ArticleDetailDto;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNullElse;

@Service
@Transactional(readOnly = true)
public class ArticleService {

    private final StkItemRepository stkItemRepository;
    private final CfgTrancheRepository trancheRepository;
    private final ComFolderRepository folderRepository;

    public ArticleService(StkItemRepository stkItemRepository, CfgTrancheRepository trancheRepository,
                           ComFolderRepository folderRepository) {
        this.stkItemRepository = stkItemRepository;
        this.trancheRepository = trancheRepository;
        this.folderRepository = folderRepository;
    }

    /**
     * Articles <b>et</b> sous-articles : un sous-article porte sa propre {@code StkHierarchyId}.
     *
     * @param statut {@link StatutArticle#DISPONIBLE} par défaut
     */
    public Page<ArticleListItemDto> findByTranche(UUID trancheId, StatutArticle statut,
                                                   List<UUID> categorieIds, List<UUID> typeIds,
                                                   UUID entiteId, UUID natureId, UUID vocationId, UUID etageId,
                                                   BigDecimal prixDe, BigDecimal prixA, BigDecimal surfaceDe, BigDecimal surfaceA,
                                                   Pageable pageable) {
        if (!trancheRepository.existsById(trancheId)) {
            throw new ResourceNotFoundException("Tranche introuvable: " + trancheId);
        }
        StatutArticle effectiveStatut = requireNonNullElse(statut, StatutArticle.DISPONIBLE);
        var spec = StkItemSpecifications.build(new FiltresArticle(List.of(trancheId), null, false, effectiveStatut,
                categorieIds, typeIds, null, false, entiteId, natureId, vocationId, etageId,
                prixDe, prixA, surfaceDe, surfaceA));
        Page<StkItem> page = stkItemRepository.findAll(spec, pageable);
        Map<UUID, ComFolder> dossiers = loadDossiers(page.getContent());
        Map<UUID, BigDecimal> superficies = loadSuperficiesSousArticles(page.getContent());
        return page.map(item -> toListItem(item, dossiers, superficies));
    }

    /** Catalogue transverse, entités actives : {@code statut} absent = tous, contrairement à {@link #findByTranche}. */
    public Page<ArticleListItemDto> findAll(String search, List<UUID> trancheIds, StatutArticle statut,
                                             List<UUID> categorieIds, List<UUID> typeIds,
                                             UUID entiteId, UUID natureId, UUID vocationId, UUID etageId,
                                             BigDecimal prixDe, BigDecimal prixA, BigDecimal surfaceDe, BigDecimal surfaceA,
                                             Pageable pageable) {
        var spec = StkItemSpecifications.build(new FiltresArticle(trancheIds, null, false, statut,
                categorieIds, typeIds, search, true, entiteId, natureId, vocationId, etageId,
                prixDe, prixA, surfaceDe, surfaceA));
        Page<StkItem> page = stkItemRepository.findAll(spec, pageable);
        Map<UUID, ComFolder> dossiers = loadDossiers(page.getContent());
        Map<UUID, BigDecimal> superficies = loadSuperficiesSousArticles(page.getContent());
        return page.map(item -> toListItem(item, dossiers, superficies));
    }

    public ArticleDetailDto findById(UUID id) {
        StkItem item = stkItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable: " + id));

        List<StkItem> sousArticlesItems = stkItemRepository.findByParentId(id);
        Map<UUID, ComFolder> dossiers = loadDossiers(Stream.concat(Stream.of(item), sousArticlesItems.stream()).toList());
        BigDecimal superficieSousArticles = sousArticlesItems.stream()
                .map(StkItem::getArea).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ArticleListItemDto> sousLots = sousArticlesItems.stream()
                .map(sousItem -> toListItem(sousItem, dossiers, Collections.emptyMap()))
                .toList();

        StkItem parent = item.getParent();
        var tranche = item.getStkHierarchy();
        var categorie = item.getStkItemCategory();
        var type = item.getStkItemType();
        var immeuble = item.getStkBuilding();
        var etage = item.getStkFloor();
        var nature = item.getStkKind();
        var presentationType = item.getStkPresentationType();
        ComFolder dossier = item.getComFolderId() != null ? dossiers.get(item.getComFolderId()) : null;
        CfgTier client = Champs.de(dossier, ComFolder::getCfgCustomer);
        Optional<PrixArticle.PrixDeReference> prixSuggere = PrixArticle.prixDeReference(item);

        return new ArticleDetailDto(
                item.getId(),
                item.getCode(),
                item.getDescription(),
                item.getAddress(),
                item.getArea(),
                item.getSalePrice(),
                item.getComSalePrice(),
                item.getSalePriceFloor(),
                item.getComSalePriceFloor(),
                statutOf(item),
                Champs.de(tranche, CfgTranche::getId),
                Champs.de(tranche, CfgTranche::getCode),
                Champs.de(tranche, CfgTranche::getDescription),
                Champs.de(parent, StkItem::getId),
                Champs.de(parent, StkItem::getCode),
                Champs.de(parent, StkItem::getDescription),
                sousLots,
                item.getTitleNumber(),
                Champs.de(categorie, StkItemCategory::getId),
                Champs.de(categorie, StkItemCategory::getCode),
                Champs.de(categorie, StkItemCategory::getDescription),
                Champs.de(type, StkItemType::getId),
                Champs.de(type, StkItemType::getCode),
                Champs.de(type, StkItemType::getDescription),
                Champs.de(dossier, ComFolder::getId),
                Champs.de(dossier, ComFolder::getCode),
                Champs.de(dossier, ComFolder::getStrStatus, StrStatus::getCode),
                Champs.de(client, CfgTier::getId),
                TierNomFormatter.nom(client),
                Champs.de(immeuble, StkBuilding::getId),
                Champs.de(immeuble, StkBuilding::getCode),
                Champs.de(immeuble, StkBuilding::getDescription),
                Champs.de(etage, StkFloor::getId),
                Champs.de(etage, StkFloor::getCode),
                Champs.de(etage, StkFloor::getDescription),
                Champs.de(nature, StkKind::getId),
                Champs.de(nature, StkKind::getCode),
                Champs.de(nature, StkKind::getDescription),
                item.getVat(),
                Champs.de(presentationType, StkPresentationType::getId),
                Champs.de(presentationType, StkPresentationType::getCode),
                Champs.de(presentationType, StkPresentationType::getDescription),
                superficieSousArticles,
                prixSuggere.map(PrixArticle.PrixDeReference::ttc).orElse(null),
                prixSuggere.map(PrixArticle.PrixDeReference::estPlancher).orElse(false)
        );
    }

    /** Même défaut DISPONIBLE que {@link #findByTranche}. */
    public Page<ArticleListItemDto> findSousArticles(UUID articleId, StatutArticle statut,
                                                       List<UUID> categorieIds, List<UUID> typeIds, Pageable pageable) {
        if (!stkItemRepository.existsById(articleId)) {
            throw new ResourceNotFoundException("Article introuvable: " + articleId);
        }
        StatutArticle effectiveStatut = requireNonNullElse(statut, StatutArticle.DISPONIBLE);
        var spec = StkItemSpecifications.build(null, articleId, false, effectiveStatut, categorieIds, typeIds);
        Page<StkItem> page = stkItemRepository.findAll(spec, pageable);
        Map<UUID, ComFolder> dossiers = loadDossiers(page.getContent());
        Map<UUID, BigDecimal> superficies = loadSuperficiesSousArticles(page.getContent());
        return page.map(item -> toListItem(item, dossiers, superficies));
    }

    /** Une requête pour toute la page, pas une par article. */
    private Map<UUID, ComFolder> loadDossiers(List<StkItem> items) {
        List<UUID> folderIds = items.stream()
                .map(StkItem::getComFolderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (folderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return folderRepository.findWithStatusAndCustomerByIdIn(folderIds).stream()
                .collect(Collectors.toMap(ComFolder::getId, Function.identity()));
    }

    /** Par id d'article parent ; absent = aucun sous-article, {@code toListItem} rend alors zéro. */
    private Map<UUID, BigDecimal> loadSuperficiesSousArticles(List<StkItem> items) {
        List<UUID> ids = items.stream().map(StkItem::getId).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, BigDecimal> result = new HashMap<>();
        for (Object[] row : stkItemRepository.sumAreaGroupedByParentIdIn(ids)) {
            result.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return result;
    }

    /** {@code ReferentielService} passe des maps vides : ses listes sont DISPONIBLE par défaut, sans dossier à lier. */
    public static ArticleListItemDto toListItem(StkItem item, Map<UUID, ComFolder> dossiersParFolderId,
                                                 Map<UUID, BigDecimal> superficiesSousArticlesParArticleId) {
        var categorie = item.getStkItemCategory();
        var type = item.getStkItemType();
        var parent = item.getParent();
        var nature = item.getStkKind();
        var etage = item.getStkFloor();
        var immeuble = item.getStkBuilding();
        var presentationType = item.getStkPresentationType();
        ComFolder dossier = item.getComFolderId() != null ? dossiersParFolderId.get(item.getComFolderId()) : null;
        CfgTier client = Champs.de(dossier, ComFolder::getCfgCustomer);
        BigDecimal superficieSousArticles = superficiesSousArticlesParArticleId.getOrDefault(item.getId(), BigDecimal.ZERO);
        Optional<PrixArticle.PrixDeReference> prixSuggere = PrixArticle.prixDeReference(item);

        return new ArticleListItemDto(
                item.getId(),
                item.getCode(),
                item.getDescription(),
                item.getArea(),
                item.getSalePrice(),
                item.getComSalePrice(),
                item.getSalePriceFloor(),
                item.getComSalePriceFloor(),
                statutOf(item),
                item.getTitleNumber(),
                Champs.de(categorie, StkItemCategory::getId),
                Champs.de(categorie, StkItemCategory::getCode),
                Champs.de(categorie, StkItemCategory::getDescription),
                Champs.de(type, StkItemType::getId),
                Champs.de(type, StkItemType::getCode),
                Champs.de(type, StkItemType::getDescription),
                parent != null,
                Champs.de(parent, StkItem::getId),
                Champs.de(dossier, ComFolder::getId),
                Champs.de(dossier, ComFolder::getCode),
                Champs.de(dossier, ComFolder::getStrStatus, StrStatus::getCode),
                Champs.de(client, CfgTier::getId),
                TierNomFormatter.nom(client),
                item.getVat(),
                Champs.de(presentationType, StkPresentationType::getId),
                Champs.de(presentationType, StkPresentationType::getCode),
                Champs.de(presentationType, StkPresentationType::getDescription),
                superficieSousArticles,
                Champs.de(nature, StkKind::getId),
                Champs.de(nature, StkKind::getCode),
                Champs.de(nature, StkKind::getDescription),
                Champs.de(etage, StkFloor::getId),
                Champs.de(etage, StkFloor::getCode),
                Champs.de(etage, StkFloor::getDescription),
                Champs.de(immeuble, StkBuilding::getId),
                Champs.de(immeuble, StkBuilding::getCode),
                Champs.de(immeuble, StkBuilding::getDescription),
                prixSuggere.map(PrixArticle.PrixDeReference::ttc).orElse(null),
                prixSuggere.map(PrixArticle.PrixDeReference::estPlancher).orElse(false)
        );
    }

    public static StatutArticle statutOf(StkItem item) {
        return item.getComFolderId() == null ? StatutArticle.DISPONIBLE : StatutArticle.OCCUPE;
    }

}
