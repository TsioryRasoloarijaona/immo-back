package com.tsix.immocrm.repositories.str;

import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StrFolderStatusHistoryRepository extends JpaRepository<StrFolderStatusHistory, UUID> {

    // GET /api/dossiers/{id}/historique — full, unpaged list (merged in-memory with the other
    // event sources before pagination; see DossierHistoriqueService).
    List<StrFolderStatusHistory> findByComFolderIdOrderByDocDateAsc(UUID comFolderId);
}
