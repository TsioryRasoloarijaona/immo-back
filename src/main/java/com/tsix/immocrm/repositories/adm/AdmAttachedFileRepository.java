package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdmAttachedFileRepository extends JpaRepository<AdmAttachedFile, UUID> {

    /** Restreint au porteur : la pièce d'un autre porteur donne vide (404), jamais son fichier. */
    Optional<AdmAttachedFile> findByIdAndCategoryId(UUID id, UUID dossierId);
}
