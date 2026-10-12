package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTrancheCoveredFloor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CfgTrancheCoveredFloorRepository extends JpaRepository<CfgTrancheCoveredFloor, UUID> {

    List<CfgTrancheCoveredFloor> findByCfgTrancheId(UUID trancheId);
}
