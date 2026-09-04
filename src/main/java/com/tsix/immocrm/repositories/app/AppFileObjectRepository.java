package com.tsix.immocrm.repositories.app;

import com.tsix.immocrm.entities.app.AppFileObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppFileObjectRepository extends JpaRepository<AppFileObject, UUID> {

    /**
     * Exclut les pièces logiquement supprimées — l'objet peut encore exister dans le bucket en
     * attendant le balayage, il ne doit pour autant plus être servi.
     */
    Optional<AppFileObject> findByAdmAttachedFileIdAndDeletedAtIsNull(UUID admAttachedFileId);
}
