package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmWebSessions;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmWebSessionsRepository
        extends JpaRepository<AdmWebSessions, UUID>, JpaSpecificationExecutor<AdmWebSessions> {

    @Query("SELECT DISTINCT s.userDesc FROM AdmWebSessions s WHERE s.userDesc <> '' ORDER BY s.userDesc ASC")
    List<String> utilisateurs();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AdmWebSessions s WHERE s.id = :id")
    Optional<AdmWebSessions> findPourEcriture(@Param("id") UUID id);
}
