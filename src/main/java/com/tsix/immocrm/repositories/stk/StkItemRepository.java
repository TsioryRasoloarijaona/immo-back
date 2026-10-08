package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface StkItemRepository extends JpaRepository<StkItem, UUID>, JpaSpecificationExecutor<StkItem> {

    // StkHierarchyId = la tranche. Les listes filtrées passent par StkItemSpecifications.
    List<StkItem> findByStkHierarchyId(UUID trancheId);

    // Sans filtre de statut : la fiche article montre tous ses sous-articles.
    List<StkItem> findByParentId(UUID parentId);

    // Un dossier peut lier plusieurs articles (principal + annexe « TER »).
    List<StkItem> findByComFolderId(UUID comFolderId);

    /** « Superficie Sous Articles » par lot de parents. Tuple : [parentId, sommeSuperficie]. */
    @Query("SELECT i.parent.id, COALESCE(SUM(i.area), 0) FROM StkItem i " +
            "WHERE i.parent.id IN :parentIds GROUP BY i.parent.id")
    List<Object[]> sumAreaGroupedByParentIdIn(List<UUID> parentIds);
}
