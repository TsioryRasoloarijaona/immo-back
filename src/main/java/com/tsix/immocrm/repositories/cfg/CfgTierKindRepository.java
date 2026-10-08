package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTierKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CfgTierKindRepository extends JpaRepository<CfgTierKind, UUID> {

    List<CfgTierKind> findByCfgTierTypeIdOrderBySortingAsc(UUID typeId);

    /** Types d'employé : sous le {@code CfgTierType} "EMP". */
    List<CfgTierKind> findByCfgTierTypeCodeOrderBySortingAsc(String tierTypeCode);
}
