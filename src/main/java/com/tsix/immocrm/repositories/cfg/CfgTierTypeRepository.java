package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTierType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfgTierTypeRepository extends JpaRepository<CfgTierType, UUID> {

    List<CfgTierType> findAllByOrderBySortingAsc();

    Optional<CfgTierType> findByCode(String code);
}
