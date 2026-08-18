package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CfgTrancheRepository extends JpaRepository<CfgTranche, UUID> {

    List<CfgTranche> findByCfgProjectId(UUID projectId);
}
