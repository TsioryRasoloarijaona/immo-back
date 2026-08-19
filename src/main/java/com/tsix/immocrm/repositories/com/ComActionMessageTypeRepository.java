package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComActionMessageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComActionMessageTypeRepository extends JpaRepository<ComActionMessageType, UUID> {
}
