package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlSettlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface StlSettlementRepository extends JpaRepository<StlSettlement, UUID>, JpaSpecificationExecutor<StlSettlement> {

    /** {@code GET /api/prospects/{id}/reglements} (GAP-06) — every settlement for this tier, across all Dossiers. */
    Page<StlSettlement> findByTierIdOrderByDocDateDesc(UUID tierId, Pageable pageable);
}
