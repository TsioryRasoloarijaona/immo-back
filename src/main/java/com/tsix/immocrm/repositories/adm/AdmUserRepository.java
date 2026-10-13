package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmUserRepository extends JpaRepository<AdmUser, UUID>, JpaSpecificationExecutor<AdmUser> {

    Optional<AdmUser> findByCode(String code);

    /** Comptes portés par ces employés ({@code CfgEmployeeId}) : un employé peut avoir plusieurs comptes. */
    List<AdmUser> findByCfgEmployeeIdIn(Collection<UUID> employeIds);
}
