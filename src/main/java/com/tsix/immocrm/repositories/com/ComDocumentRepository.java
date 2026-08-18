package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComDocumentRepository extends JpaRepository<ComDocument, UUID> {

    List<ComDocument> findByComFolderIdOrderByDocDateAsc(UUID comFolderId);
}
