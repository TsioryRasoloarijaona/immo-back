package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface StkItemRepository extends JpaRepository<StkItem, UUID>, JpaSpecificationExecutor<StkItem> {

    // StkItem.stkHierarchy (StkHierarchyId) is the FK to the CfgTranche an article belongs to.
    // Kept as a plain derived query (no filtering needed) for ComFolderItem/Dossier-adjacent
    // lookups; the filtered article/sous-article listings go through StkItemSpecifications
    // instead (tranche/parent/statut/categorie/type all combine there).
    List<StkItem> findByStkHierarchyId(UUID trancheId);

    // Sub-lots (e.g. cave/parking annexed to a main lot) via StkItem.parent (ParentId) — used
    // unfiltered by ArticleService#findById (ArticleDetailDto.sousLots shows every sous-article
    // regardless of statut).
    List<StkItem> findByParentId(UUID parentId);

    // A Dossier (ComFolder) can have several articles (confirmed: e.g. MBR 2022/00205 →
    // "MB 18 1 04" + its annex "MB 18 1 04 TER"), linked via StkItem.ComFolderId.
    List<StkItem> findByComFolderId(UUID comFolderId);

    /**
     * "Superficie Sous Articles" (legacy "Articles" grid, instructions/from_front.md #4) —
     * batch aggregate of {@code Area} across every sous-article of a page/list of parent
     * articles, keyed by {@code parentId}, in one round trip (same batching precedent as
     * {@code ArticleService.loadDossiers}). Tuple: [parentId, sommeSuperficie].
     */
    @Query("SELECT i.parent.id, COALESCE(SUM(i.area), 0) FROM StkItem i " +
            "WHERE i.parent.id IN :parentIds GROUP BY i.parent.id")
    List<Object[]> sumAreaGroupedByParentIdIn(List<UUID> parentIds);
}
