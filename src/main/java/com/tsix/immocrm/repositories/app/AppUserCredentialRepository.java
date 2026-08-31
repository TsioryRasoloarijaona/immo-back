package com.tsix.immocrm.repositories.app;

import com.tsix.immocrm.entities.app.AppUserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserCredentialRepository extends JpaRepository<AppUserCredential, UUID> {

    Optional<AppUserCredential> findByAdmUserId(UUID admUserId);

    /** Batch variant of {@link #findByAdmUserId} — avoids N+1 when listing several users at once. */
    List<AppUserCredential> findByAdmUserIdIn(List<UUID> admUserIds);
}
