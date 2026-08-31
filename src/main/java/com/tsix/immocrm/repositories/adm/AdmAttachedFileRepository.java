package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdmAttachedFileRepository extends JpaRepository<AdmAttachedFile, UUID> {

    /** Scoped by dossier ({@code CategoryId}) so one dossier can't fetch another's file by id. */
    Optional<AdmAttachedFile> findByIdAndCategoryId(UUID id, UUID dossierId);
}
