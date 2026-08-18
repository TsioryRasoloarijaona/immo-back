package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComActionRepository extends JpaRepository<ComAction, UUID> {
}
