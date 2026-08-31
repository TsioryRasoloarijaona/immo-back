package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgRelationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CfgRelationTypeRepository extends JpaRepository<CfgRelationType, UUID> {
}
