package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComSettlementCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComSettlementCategoryRepository extends JpaRepository<ComSettlementCategory, UUID> {

    Optional<ComSettlementCategory> findByCodeAndStkVocationId(String code, UUID vocationId);

    /** Catégories « frais de vente » d'OMRAN : {@code IsCredit = 1 AND IsSaleExpense = 1}. */
    List<ComSettlementCategory> findByIsCreditTrueAndIsSaleExpenseTrueOrderByCodeAsc();
}
