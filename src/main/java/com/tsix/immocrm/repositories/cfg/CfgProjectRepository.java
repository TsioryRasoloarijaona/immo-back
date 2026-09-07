package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CfgProjectRepository extends JpaRepository<CfgProject, UUID> {

    /** Projets dont l'entité juridique est en exploitation — voir {@code CfgCompany.isActive}. */
    @Query("SELECT p FROM CfgProject p WHERE p.cfgCompany.isActive = true")
    List<CfgProject> findDesEntitesActives();

    @Query("SELECT p FROM CfgProject p WHERE p.cfgCompany.isActive = true")
    Page<CfgProject> findDesEntitesActives(Pageable pageable);
}
