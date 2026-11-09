package com.tsix.immocrm.repositories.prj;

import com.tsix.immocrm.entities.prj.PrjMarket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PrjMarketRepository extends JpaRepository<PrjMarket, UUID>, JpaSpecificationExecutor<PrjMarket> {

    @Override
    @EntityGraph(attributePaths = {"prjProject", "prjMarketType", "cfgSupplier", "stlCategory", "strStatus", "strEntity"})
    Page<PrjMarket> findAll(Specification<PrjMarket> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"prjProject", "prjMarketType", "cfgSupplier", "stlCategory", "strStatus", "strEntity"})
    Optional<PrjMarket> findByIdAndPrjProjectIdIn(UUID id, Collection<UUID> projets);
}
