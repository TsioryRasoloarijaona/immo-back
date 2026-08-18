package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTrancheStkItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CfgTrancheStkItemRepository extends JpaRepository<CfgTrancheStkItem, UUID> {
}
