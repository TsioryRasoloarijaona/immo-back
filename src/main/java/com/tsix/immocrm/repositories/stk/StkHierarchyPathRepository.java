package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkHierarchyPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StkHierarchyPathRepository extends JpaRepository<StkHierarchyPath, UUID> {

    /** Tracé d'un plan, avec son article. */
    interface TraceArticle {
        UUID getArticleId();

        String getCode();

        String getD();

        Boolean getSousArticle();
    }

    // Un étage est partagé entre tranches : filtrer aussi par la tranche de l'article.
    @Query("SELECT i.id AS articleId, i.code AS code, h.svgPath AS d, i.isUnderItem AS sousArticle "
            + "FROM StkHierarchyPath h JOIN StkItem i ON i.id = h.stkItemId "
            + "WHERE h.stkFloorId = :etageId AND i.stkHierarchy.id = :trancheId ORDER BY i.code")
    List<TraceArticle> tracesDuPlan(UUID etageId, UUID trancheId);

    @Query("SELECT h FROM StkHierarchyPath h WHERE h.stkItemId IN "
            + "(SELECT i.id FROM StkItem i WHERE i.stkHierarchy.id = :trancheId)")
    List<StkHierarchyPath> tracesDeLaTranche(UUID trancheId);

    List<StkHierarchyPath> findByStkFloorIdAndStkItemIdIn(UUID etageId, Collection<UUID> articleIds);
}
