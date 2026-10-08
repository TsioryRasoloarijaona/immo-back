package com.tsix.immocrm.repositories.app;

import com.tsix.immocrm.entities.app.AppUserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserCredentialRepository extends JpaRepository<AppUserCredential, UUID> {

    /**
     * {@code readOnly} décide de la base : appelée hors transaction par le filtre JWT, une requête dérivée
     * irait sur la source de vérité (401 sur toute requête si l'utilisateur n'y est pas). La révocation
     * ({@code TokenVersion}, écrit sur la source) atteint la copie via {@code config/RemonteeLecture}.
     */
    @Transactional(readOnly = true)
    Optional<AppUserCredential> findByAdmUserId(UUID admUserId);

    List<AppUserCredential> findByAdmUserIdIn(List<UUID> admUserIds);
}
