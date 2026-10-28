package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComFolderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ComFolderItemRepository extends JpaRepository<ComFolderItem, UUID> {

    /** Article principal et sous-articles ({@code IsUnderItem}), chacun avec son prix figé pour ce dossier. */
    List<ComFolderItem> findByComFolderId(UUID comFolderId);

    /** Codes des articles d'une catégorie portés par un lot de dossiers. Tuple : [dossierId, code de l'article]. */
    @Query("SELECT f.comFolder.id, f.stkItem.code FROM ComFolderItem f WHERE f.comFolder.id IN :dossierIds "
            + "AND f.stkItem.stkItemCategory.code = :categorie")
    List<Object[]> codesByComFolderIdInAndCategorie(List<UUID> dossierIds, String categorie);
}
