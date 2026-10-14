package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComModification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComModificationRepository extends JpaRepository<ComModification, UUID> {

    List<ComModification> findByComFolderIdOrderByDemandDateDesc(UUID comFolderId);
}
