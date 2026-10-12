package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfgProjectRepository extends JpaRepository<CfgProject, UUID> {

    /** Entité en exploitation ; chemin implicite = jointure interne, un projet sans société est exclu. */
    @Query("SELECT p FROM CfgProject p WHERE p.cfgCompany.isActive = true")
    List<CfgProject> findDesEntitesActives();

    @Query("SELECT p FROM CfgProject p WHERE p.cfgCompany.isActive = true")
    Page<CfgProject> findDesEntitesActives(Pageable pageable);

    /** Unicité du code (collation French_CI_AS : « = » ignore déjà la casse). */
    Optional<CfgProject> findFirstByCodeAndIdNot(String code, UUID id);
}
