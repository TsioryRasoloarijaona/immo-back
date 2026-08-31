package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComFolderCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComFolderCustomerRepository extends JpaRepository<ComFolderCustomer, UUID> {

    /** Co-acquéreurs additionnels d'un dossier (au-delà de {@code ComFolder.cfgCustomer}) — voir "FICHE BANCAIRE". */
    List<ComFolderCustomer> findByComFolderId(UUID comFolderId);
}
