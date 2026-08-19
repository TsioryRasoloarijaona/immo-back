package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.dto.patrimoine.ArticleDetailDto;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ArticleService {

    private final StkItemRepository stkItemRepository;
    private final CfgTrancheRepository trancheRepository;

    public ArticleService(StkItemRepository stkItemRepository, CfgTrancheRepository trancheRepository) {
        this.stkItemRepository = stkItemRepository;
        this.trancheRepository = trancheRepository;
    }

    /**
     * @param statut filter; defaults to {@link StatutArticle#DISPONIBLE} when {@code null}
     */
    public Page<ArticleListItemDto> findByTranche(UUID trancheId, StatutArticle statut, Pageable pageable) {
        if (!trancheRepository.existsById(trancheId)) {
            throw new ResourceNotFoundException("Tranche introuvable: " + trancheId);
        }
        StatutArticle effectiveStatut = statut != null ? statut : StatutArticle.DISPONIBLE;
        Page<StkItem> items = effectiveStatut == StatutArticle.DISPONIBLE
                ? stkItemRepository.findByStkHierarchyIdAndComFolderIdIsNull(trancheId, pageable)
                : stkItemRepository.findByStkHierarchyIdAndComFolderIdIsNotNull(trancheId, pageable);
        return items.map(ArticleService::toListItem);
    }

    public ArticleDetailDto findById(UUID id) {
        StkItem item = stkItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable: " + id));

        List<ArticleListItemDto> sousLots = stkItemRepository.findByParentId(id).stream()
                .map(ArticleService::toListItem)
                .toList();

        return new ArticleDetailDto(
                item.getId(),
                item.getCode(),
                item.getDescription(),
                item.getAddress(),
                item.getArea(),
                item.getSalePrice(),
                item.getComSalePrice(),
                statutOf(item),
                item.getStkHierarchy() != null ? item.getStkHierarchy().getId() : null,
                item.getParent() != null ? item.getParent().getId() : null,
                sousLots,
                item.getTitleNumber(),
                item.getStkItemCategory() != null ? item.getStkItemCategory().getCode() : null,
                item.getStkItemType() != null ? item.getStkItemType().getCode() : null
        );
    }

    private static ArticleListItemDto toListItem(StkItem item) {
        return new ArticleListItemDto(
                item.getId(),
                item.getCode(),
                item.getDescription(),
                item.getArea(),
                item.getSalePrice(),
                item.getComSalePrice(),
                statutOf(item),
                item.getTitleNumber(),
                item.getStkItemCategory() != null ? item.getStkItemCategory().getCode() : null,
                item.getStkItemType() != null ? item.getStkItemType().getCode() : null
        );
    }

    private static StatutArticle statutOf(StkItem item) {
        return item.getComFolderId() == null ? StatutArticle.DISPONIBLE : StatutArticle.OCCUPE;
    }
}
