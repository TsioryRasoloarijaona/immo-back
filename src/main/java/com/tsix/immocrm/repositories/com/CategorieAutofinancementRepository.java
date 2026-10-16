package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComSettlementCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Catégorie « Autofinancement » au crédit d'une vocation, pour une ligne d'échéancier créée par le réajustement. */
public interface CategorieAutofinancementRepository extends JpaRepository<ComSettlementCategory, UUID> {

    Optional<ComSettlementCategory> findFirstByStkVocationIdAndIsSaleAutoFinancmentTrueAndIsCreditTrueOrderByCodeAsc(
            UUID vocationId);
}
