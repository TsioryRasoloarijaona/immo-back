package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComFolderCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ComFolderCustomerRepository extends JpaRepository<ComFolderCustomer, UUID> {

    /** Co-acquéreurs en plus de {@code ComFolder.cfgCustomer}. */
    List<ComFolderCustomer> findByComFolderId(UUID comFolderId);

    /** Co-acquéreurs de plusieurs dossiers (≤ 2 000 ids), tiers chargé. */
    @Query("SELECT c FROM ComFolderCustomer c JOIN FETCH c.cfgTier WHERE c.comFolder.id IN :dossierIds")
    List<ComFolderCustomer> findByComFolderIdIn(List<UUID> dossierIds);
}
