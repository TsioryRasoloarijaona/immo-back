package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComDocumentPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComDocumentPaymentMethodRepository extends JpaRepository<ComDocumentPaymentMethod, UUID> {

    /** {@code ComDocumentPaymentMethod.GetDocuments} : date décroissante. */
    List<ComDocumentPaymentMethod> findByComFolderIdOrderByDocDateDesc(UUID comFolderId);

    boolean existsByComFolderId(UUID comFolderId);
}
