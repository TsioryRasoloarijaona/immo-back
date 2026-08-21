package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdmUserRepository extends JpaRepository<AdmUser, UUID> {

    /** {@code POST /api/auth/connexion} — login lookup by username. */
    Optional<AdmUser> findByCode(String code);
}
