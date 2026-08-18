package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComFolderStatusPaymentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComFolderStatusPaymentTypeRepository extends JpaRepository<ComFolderStatusPaymentType, UUID> {
}
