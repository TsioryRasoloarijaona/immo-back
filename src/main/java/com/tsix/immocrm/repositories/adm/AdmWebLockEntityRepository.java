package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.dto.administration.LigneFicheVerrouilleeDto;
import com.tsix.immocrm.entities.adm.AdmWebLockEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmWebLockEntityRepository extends JpaRepository<AdmWebLockEntity, UUID> {

    String FILTRES = """
            FROM AdmWebLockEntity f LEFT JOIN AdmWebSessions s ON s.id = f.sessionId
            WHERE (:stateEgal IS NULL OR f.state = :stateEgal)
            AND (:stateExclu IS NULL OR f.state <> :stateExclu)
            AND (:debut IS NULL OR f.lockDate >= :debut)
            AND (:finExclue IS NULL OR f.lockDate < :finExclue)
            AND (:motif IS NULL OR f.code LIKE :motif OR f.description LIKE :motif OR s.userDesc LIKE :motif)""";

    @Query(value = "SELECT new com.tsix.immocrm.dto.administration.LigneFicheVerrouilleeDto(f.id, f.code, f.description, "
            + "f.lockDate, s.userDesc, f.state) " + FILTRES + " ORDER BY f.lockDate DESC, f.id ASC",
            countQuery = "SELECT COUNT(f) " + FILTRES)
    Page<LigneFicheVerrouilleeDto> lignes(Integer stateEgal, Integer stateExclu, LocalDateTime debut,
                                          LocalDateTime finExclue, String motif, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM AdmWebLockEntity f WHERE f.id = :id")
    Optional<AdmWebLockEntity> findPourEcriture(@Param("id") UUID id);

    List<AdmWebLockEntity> findByState(int state);
}
