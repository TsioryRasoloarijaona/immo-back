package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTierKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CfgTierKindRepository extends JpaRepository<CfgTierKind, UUID> {

    List<CfgTierKind> findByCfgTierTypeIdOrderBySortingAsc(UUID typeId);

    /**
     * {@code GET /api/referentiels/types-employe} — kinds under the "EMP" {@code CfgTierType},
     * i.e. the employee types read from Omran ({@code typeEmployeCode} of GET /api/utilisateurs).
     */
    List<CfgTierKind> findByCfgTierTypeCodeOrderBySortingAsc(String tierTypeCode);
}
