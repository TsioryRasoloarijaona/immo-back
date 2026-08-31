package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface AdmUserRepository extends JpaRepository<AdmUser, UUID>, JpaSpecificationExecutor<AdmUser> {

    /** {@code POST /api/auth/connexion} — login lookup by username. */
    Optional<AdmUser> findByCode(String code);
}
