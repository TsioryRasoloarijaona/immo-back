package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComFolderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComFolderItemRepository extends JpaRepository<ComFolderItem, UUID> {
}
