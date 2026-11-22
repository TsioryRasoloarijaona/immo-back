package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlSettlementHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StlSettlementHistoryRepository extends JpaRepository<StlSettlementHistory, UUID> {

    List<StlSettlementHistory> findByStlSettlementId(UUID reglementId);
}
