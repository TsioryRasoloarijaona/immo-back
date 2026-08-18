package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlSettlementRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StlSettlementRelationRepository extends JpaRepository<StlSettlementRelation, UUID> {
}
