package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComFolderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComFolderItemRepository extends JpaRepository<ComFolderItem, UUID> {

    /** Article principal et sous-articles ({@code IsUnderItem}), chacun avec son prix figé pour ce dossier. */
    List<ComFolderItem> findByComFolderId(UUID comFolderId);
}
