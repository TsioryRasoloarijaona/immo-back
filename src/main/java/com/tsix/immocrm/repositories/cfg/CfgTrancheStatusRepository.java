package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTrancheStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CfgTrancheStatusRepository extends JpaRepository<CfgTrancheStatus, UUID> {
}
