package com.tsix.immocrm.repositories.app;

import com.tsix.immocrm.entities.app.AppUserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserCredentialRepository extends JpaRepository<AppUserCredential, UUID> {

    Optional<AppUserCredential> findByAdmUserId(UUID admUserId);
}
