package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CfgProjectRepository extends JpaRepository<CfgProject, UUID> {

    /** Projets dont l'entité juridique ({@code CfgCompany.Code}) fait partie de {@code codes} — voir {@link com.tsix.immocrm.domain.EntitesJuridiquesActives}. */
    List<CfgProject> findByCfgCompany_CodeIn(Collection<String> codes);

    Page<CfgProject> findByCfgCompany_CodeIn(Collection<String> codes, Pageable pageable);
}
