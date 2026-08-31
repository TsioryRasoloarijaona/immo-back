package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ComProspectionRepository extends JpaRepository<ComProspection, UUID>, JpaSpecificationExecutor<ComProspection> {

    // Pré-prospection: CfgProspectId IS NULL AND CfgCommercialId IS NULL (confirmed).
    List<ComProspection> findByCfgProspectIsNullAndCfgCommercialIsNull();

    Page<ComProspection> findByCfgProspectIsNullAndCfgCommercialIsNull(Pageable pageable);

    /** Real prospections (excludes pré-prospections), for {@code GET /api/dashboard}. */
    @Query("SELECT COUNT(p) FROM ComProspection p WHERE NOT (p.cfgProspect IS NULL AND p.cfgCommercial IS NULL)")
    long countExcludingPreProspections();

    /** Real, non-closed prospections, for {@code GET /api/dashboard}. */
    @Query("SELECT COUNT(p) FROM ComProspection p WHERE NOT (p.cfgProspect IS NULL AND p.cfgCommercial IS NULL) " +
            "AND (p.isClosed IS NULL OR p.isClosed = false)")
    long countOpenExcludingPreProspections();

    /** {@code GET /api/prospects/{id}/prospections} (GAP-06). */
    Page<ComProspection> findByCfgProspectId(UUID prospectId, Pageable pageable);

    /**
     * {@code GET /api/hotline/recherche} — every {@code ComProspection} whose {@code TempTel}
     * (the free-text phone captured before a caller has a {@code CfgTier} record, e.g.
     * pré-prospections) matches the given number. Same format-agnostic normalization as
     * {@link com.tsix.immocrm.repositories.cfg.CfgTierRepository#findByNormalizedPhone}.
     */
    @Query("SELECT p FROM ComProspection p WHERE "
            + "right(replace(replace(replace(replace(replace(replace(coalesce(p.tempTel, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :normalizedPhone")
    Page<ComProspection> findByNormalizedTempTel(@Param("normalizedPhone") String normalizedPhone, Pageable pageable);
}
