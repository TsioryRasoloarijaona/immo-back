package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTrancheStanding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CfgTrancheStandingRepository extends JpaRepository<CfgTrancheStanding, UUID> {
}
