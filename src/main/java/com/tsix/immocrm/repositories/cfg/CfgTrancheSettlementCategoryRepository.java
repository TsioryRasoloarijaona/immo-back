package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CfgTrancheSettlementCategoryRepository extends JpaRepository<CfgTrancheSettlementCategory, UUID> {
}
