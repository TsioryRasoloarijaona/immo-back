package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTierKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfgTierKindRepository extends JpaRepository<CfgTierKind, UUID> {

    List<CfgTierKind> findByCfgTierTypeIdOrderBySortingAsc(UUID typeId);

    /**
     * {@code GET /api/referentiels/types-employe} — kinds under the "EMP" {@code CfgTierType},
     * i.e. the codes assignable to {@code POST /api/utilisateurs}'s {@code typeEmployeCode}.
     */
    List<CfgTierKind> findByCfgTierTypeCodeOrderBySortingAsc(String tierTypeCode);

    /** {@code POST /api/utilisateurs} — resolves the requested employee kind by its business code. */
    Optional<CfgTierKind> findByCode(String code);
}
