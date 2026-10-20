package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlSettlementLineSuivi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface StlSettlementLineSuiviRepository
        extends JpaRepository<StlSettlementLineSuivi, UUID>, JpaSpecificationExecutor<StlSettlementLineSuivi> {

    @Override
    @EntityGraph(attributePaths = {"tiers", "banque"})
    Page<StlSettlementLineSuivi> findAll(Specification<StlSettlementLineSuivi> spec, Pageable pageable);
}
