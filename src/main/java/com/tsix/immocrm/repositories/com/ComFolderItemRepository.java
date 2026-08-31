package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComFolderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComFolderItemRepository extends JpaRepository<ComFolderItem, UUID> {

    /**
     * Every {@code ComFolderItem} row of a Dossier — the main article (IsUnderItem=false) plus
     * its sous-articles (IsUnderItem=true) — each carrying its own per-dossier price/discount
     * snapshot (unlike {@code StkItem}, which has no such override). Used by the "Fiche Dossier
     * de Vente" SOUS ARTICLES table.
     */
    List<ComFolderItem> findByComFolderId(UUID comFolderId);
}
