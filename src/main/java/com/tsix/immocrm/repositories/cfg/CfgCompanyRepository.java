package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CfgCompanyRepository extends JpaRepository<CfgCompany, UUID> {

    /** {@code GET /api/referentiels/entites} — restreint aux entités actives, voir {@link com.tsix.immocrm.domain.EntitesJuridiquesActives}. */
    List<CfgCompany> findByCodeInOrderByDescriptionAsc(Collection<String> codes);
}
