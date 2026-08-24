package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmRoleRepository extends JpaRepository<AdmRole, UUID> {

    /** {@code POST /api/utilisateurs} — resolves the requested role codes in one round-trip. */
    List<AdmRole> findByCodeIn(List<String> codes);
}
