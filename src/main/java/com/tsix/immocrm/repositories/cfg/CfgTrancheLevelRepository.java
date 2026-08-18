package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTrancheLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CfgTrancheLevelRepository extends JpaRepository<CfgTrancheLevel, UUID> {
}
