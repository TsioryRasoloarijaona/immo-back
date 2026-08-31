package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmUserRoleRepository extends JpaRepository<AdmUserRole, UUID> {

    /** All roles assigned to a user — drives the JWT's {@code roles} claim. */
    List<AdmUserRole> findByAdmUserId(UUID admUserId);

    /** Batch variant of {@link #findByAdmUserId} — avoids N+1 when listing several users at once. */
    List<AdmUserRole> findByAdmUserIdIn(List<UUID> admUserIds);
}
