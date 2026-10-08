package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlSettlementType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StlSettlementTypeRepository extends JpaRepository<StlSettlementType, UUID> {

    /** Pas de colonne Sorting : tri alphabétique. */
    List<StlSettlementType> findAllByOrderByDescriptionAsc();
}
