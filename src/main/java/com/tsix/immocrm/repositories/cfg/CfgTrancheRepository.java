package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CfgTrancheRepository extends JpaRepository<CfgTranche, UUID> {

    List<CfgTranche> findByCfgProjectId(UUID projectId);

    Page<CfgTranche> findByCfgProjectId(UUID projectId, Pageable pageable);

    /** Tranches dont l'entité juridique du projet parent est en exploitation — voir {@code CfgCompany.isActive}. */
    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.cfgCompany.isActive = true")
    List<CfgTranche> findDesEntitesActives();

    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.cfgCompany.isActive = true")
    Page<CfgTranche> findDesEntitesActives(Pageable pageable);

    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.id = :projetId AND t.cfgProject.cfgCompany.isActive = true")
    List<CfgTranche> findDesEntitesActivesParProjet(@Param("projetId") UUID projetId);

    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.id = :projetId AND t.cfgProject.cfgCompany.isActive = true")
    Page<CfgTranche> findDesEntitesActivesParProjet(@Param("projetId") UUID projetId, Pageable pageable);
}
