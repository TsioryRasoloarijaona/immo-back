package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StlSettlementRepository extends JpaRepository<StlSettlement, UUID> {
}
