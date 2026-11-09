package com.tsix.immocrm.repositories.prj;

import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrjWorkingOutRepository extends JpaRepository<PrjWorkingOut, UUID>, JpaSpecificationExecutor<PrjWorkingOut> {

    String PROJET = "prjMarket.prjProject";
    String FOURNISSEUR_LOT = "prjMarket.cfgSupplier";
    String TIERS = "cfgTier";
    String FACTURE = "buyDocument";

    @Override
    @EntityGraph(attributePaths = {PROJET, FOURNISSEUR_LOT, TIERS, FACTURE})
    Page<PrjWorkingOut> findAll(Specification<PrjWorkingOut> spec, Pageable pageable);

    @EntityGraph(attributePaths = {PROJET, FOURNISSEUR_LOT, TIERS, FACTURE})
    List<PrjWorkingOut> findByPrjMarketIdOrderBySortingAscDocDateAsc(UUID lotId);

    @EntityGraph(attributePaths = {PROJET, FOURNISSEUR_LOT, TIERS, FACTURE})
    Optional<PrjWorkingOut> findByIdAndPrjMarketPrjProjectIdIn(UUID id, Collection<UUID> projets);
}
