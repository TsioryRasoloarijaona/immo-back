package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CfgProjectRepository extends JpaRepository<CfgProject, UUID> {
}
