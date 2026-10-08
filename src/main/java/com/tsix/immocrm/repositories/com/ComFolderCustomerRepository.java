package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComFolderCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComFolderCustomerRepository extends JpaRepository<ComFolderCustomer, UUID> {

    /** Co-acquéreurs en plus de {@code ComFolder.cfgCustomer}. */
    List<ComFolderCustomer> findByComFolderId(UUID comFolderId);
}
