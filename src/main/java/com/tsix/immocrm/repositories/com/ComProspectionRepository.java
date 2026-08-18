package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ComProspectionRepository extends JpaRepository<ComProspection, UUID>, JpaSpecificationExecutor<ComProspection> {

    // Pré-prospection: CfgProspectId IS NULL AND CfgCommercialId IS NULL (confirmed).
    List<ComProspection> findByCfgProspectIsNullAndCfgCommercialIsNull();

    /** Real prospections (excludes pré-prospections), for {@code GET /api/dashboard}. */
    @Query("SELECT COUNT(p) FROM ComProspection p WHERE NOT (p.cfgProspect IS NULL AND p.cfgCommercial IS NULL)")
    long countExcludingPreProspections();

    /** Real, non-closed prospections, for {@code GET /api/dashboard}. */
    @Query("SELECT COUNT(p) FROM ComProspection p WHERE NOT (p.cfgProspect IS NULL AND p.cfgCommercial IS NULL) " +
            "AND (p.isClosed IS NULL OR p.isClosed = false)")
    long countOpenExcludingPreProspections();
}
