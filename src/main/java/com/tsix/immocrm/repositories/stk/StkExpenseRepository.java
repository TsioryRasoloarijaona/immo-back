package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkExpense;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StkExpenseRepository extends JpaRepository<StkExpense, UUID> {

    List<StkExpense> findByStkItemIdOrderByDateAscIdAsc(UUID terrainId);

    /** PUT des frais : une validation concurrente attend, sinon la ligne réécrite remettrait IsValidated à 0. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM StkExpense e WHERE e.stkItem.id = :terrainId")
    List<StkExpense> findPourEnregistrement(@Param("terrainId") UUID terrainId);

    /** Coût total du terrain : Σ des montants de ses frais, validés ou non. */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM StkExpense e WHERE e.stkItem.id = :terrainId")
    BigDecimal coutTotal(@Param("terrainId") UUID terrainId);

    /** Verrou de la validation pour règlement : une validation concurrente du même frais attend celle-ci. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM StkExpense e WHERE e.id = :id AND e.stkItem.id = :terrainId")
    Optional<StkExpense> findPourValidation(@Param("id") UUID id, @Param("terrainId") UUID terrainId);
}
