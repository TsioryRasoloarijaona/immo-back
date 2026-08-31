package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CfgTrancheRepository extends JpaRepository<CfgTranche, UUID> {

    List<CfgTranche> findByCfgProjectId(UUID projectId);

    Page<CfgTranche> findByCfgProjectId(UUID projectId, Pageable pageable);

    /** Tranches dont l'entité juridique du projet parent ({@code CfgCompany.Code}) fait partie de {@code codes} — voir {@link com.tsix.immocrm.domain.EntitesJuridiquesActives}. */
    List<CfgTranche> findByCfgProject_CfgCompany_CodeIn(Collection<String> codes);

    Page<CfgTranche> findByCfgProject_CfgCompany_CodeIn(Collection<String> codes, Pageable pageable);

    List<CfgTranche> findByCfgProjectIdAndCfgProject_CfgCompany_CodeIn(UUID projectId, Collection<String> codes);

    Page<CfgTranche> findByCfgProjectIdAndCfgProject_CfgCompany_CodeIn(UUID projectId, Collection<String> codes, Pageable pageable);
}
