package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmUserRepository extends JpaRepository<AdmUser, UUID>, JpaSpecificationExecutor<AdmUser> {

    Optional<AdmUser> findByCode(String code);

    /** Comptes portés par ces employés ({@code CfgEmployeeId}) : un employé peut avoir plusieurs comptes. */
    List<AdmUser> findByCfgEmployeeIdIn(Collection<UUID> employeIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM AdmUser u WHERE u.id = :id")
    Optional<AdmUser> findPourEcriture(@Param("id") UUID id);

    /** {@code Code} est sensible à la casse selon la collation, OMRAN connecte par UPPER. */
    @Query("SELECT COUNT(u) FROM AdmUser u WHERE u.codeMajuscule = UPPER(:code)")
    long compterParCode(@Param("code") String code);

    /** {@code Pass} n'est pas mappé : l'INSERT laisse jouer le DEFAULT N'' de la base, OMRAN attend NULL (R6). */
    @Modifying
    @Query(value = "UPDATE AdmUser SET Pass = NULL WHERE PKey = :id", nativeQuery = true)
    void retirerDefautPass(@Param("id") UUID id);
}
