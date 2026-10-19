package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkItemEtatVentes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface StkItemEtatVentesRepository
        extends JpaRepository<StkItemEtatVentes, UUID>, JpaSpecificationExecutor<StkItemEtatVentes> {

    @Override
    @EntityGraph(attributePaths = {"tranche", "categorie", "type", "etage", "immeuble", "dossier",
            "dossier.strStatus", "dossier.cfgCustomer", "dossier.cfgCommercial"})
    Page<StkItemEtatVentes> findAll(Specification<StkItemEtatVentes> spec, Pageable pageable);
}
