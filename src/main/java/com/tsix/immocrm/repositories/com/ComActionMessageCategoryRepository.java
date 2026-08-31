package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ComActionMessageCategoryRepository extends JpaRepository<ComActionMessageCategory, UUID> {

    Optional<ComActionMessageCategory> findByCode(String code);
}
