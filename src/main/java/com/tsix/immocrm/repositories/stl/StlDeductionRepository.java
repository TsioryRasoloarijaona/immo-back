package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlDeduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StlDeductionRepository extends JpaRepository<StlDeduction, UUID> {

    List<StlDeduction> findByStlSettlementIdOrderByAmountBaseAsc(UUID reglementId);
}
