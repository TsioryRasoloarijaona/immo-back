package com.tsix.immocrm.repositories.app;

import com.tsix.immocrm.entities.app.AppFileObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppFileObjectRepository extends JpaRepository<AppFileObject, UUID> {

    /** Une pièce supprimée peut encore être dans le bucket (balayage différé) : ne plus la servir. */
    Optional<AppFileObject> findByAdmAttachedFileIdAndDeletedAtIsNull(UUID admAttachedFileId);
}
