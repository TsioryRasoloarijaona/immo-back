package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CfgTierRepository extends JpaRepository<CfgTier, UUID>, JpaSpecificationExecutor<CfgTier> {
}
