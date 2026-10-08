package com.tsix.immocrm.repositories.str;

import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StrFolderStatusHistoryRepository extends JpaRepository<StrFolderStatusHistory, UUID> {

    // Non paginée : fusionnée en mémoire avec les autres sources avant pagination (DossierHistoriqueService).
    List<StrFolderStatusHistory> findByComFolderIdOrderByDocDateAsc(UUID comFolderId);
}
