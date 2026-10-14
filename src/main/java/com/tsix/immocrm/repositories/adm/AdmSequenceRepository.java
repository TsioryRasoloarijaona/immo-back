package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AdmSequenceRepository extends JpaRepository<AdmSequence, UUID> {

    Optional<AdmSequence> findByCode(String code);

    /** Lecture pour incrément : UPDLOCK sur SQL Server, une transaction concurrente attend la fin de celle-ci. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AdmSequence s WHERE s.code = :code")
    Optional<AdmSequence> findPourIncrementByCode(@Param("code") String code);
}
