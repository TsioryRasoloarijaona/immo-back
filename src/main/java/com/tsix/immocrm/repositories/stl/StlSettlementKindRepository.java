package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlSettlementKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StlSettlementKindRepository extends JpaRepository<StlSettlementKind, UUID> {
}
