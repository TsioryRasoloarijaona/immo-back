package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComSettlementCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ComSettlementCategoryRepository extends JpaRepository<ComSettlementCategory, UUID> {

    Optional<ComSettlementCategory> findByCodeAndStkVocationId(String code, UUID vocationId);

    /** Deux catégories de résiliation en prod : celle du désistement est au débit, jamais celle de l'annulation. */
    Optional<ComSettlementCategory> findFirstByCodeAndIsSaleResiliationTrueAndIsCreditFalse(String code);
}
