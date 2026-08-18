package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StkItemRepository extends JpaRepository<StkItem, UUID> {

    // StkItem.stkHierarchy (StkHierarchyId) is the FK to the CfgTranche an article belongs to.
    List<StkItem> findByStkHierarchyId(UUID trancheId);

    List<StkItem> findByStkHierarchyIdAndComFolderIdIsNull(UUID trancheId);

    List<StkItem> findByStkHierarchyIdAndComFolderIdIsNotNull(UUID trancheId);

    // Sub-lots (e.g. cave/parking annexed to a main lot) via StkItem.parent (ParentId).
    List<StkItem> findByParentId(UUID parentId);

    // A Dossier (ComFolder) can have several articles (confirmed: e.g. MBR 2022/00205 →
    // "MB 18 1 04" + its annex "MB 18 1 04 TER"), linked via StkItem.ComFolderId.
    List<StkItem> findByComFolderId(UUID comFolderId);
}
