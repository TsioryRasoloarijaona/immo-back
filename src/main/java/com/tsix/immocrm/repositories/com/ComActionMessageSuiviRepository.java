package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComActionMessageSuivi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ComActionMessageSuiviRepository
        extends JpaRepository<ComActionMessageSuivi, UUID>, JpaSpecificationExecutor<ComActionMessageSuivi> {
}
