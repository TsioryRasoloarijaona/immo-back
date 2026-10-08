package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmUserRoleRepository extends JpaRepository<AdmUserRole, UUID> {

    /** Alimente le claim {@code roles} du JWT. */
    List<AdmUserRole> findByAdmUserId(UUID admUserId);

    List<AdmUserRole> findByAdmUserIdIn(List<UUID> admUserIds);
}
