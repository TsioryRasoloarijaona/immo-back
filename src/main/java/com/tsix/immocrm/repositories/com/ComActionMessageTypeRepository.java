package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComActionMessageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ComActionMessageTypeRepository extends JpaRepository<ComActionMessageType, UUID> {

    /** Par code métier — sert les valeurs par défaut de l'import CSV des leads. */
    Optional<ComActionMessageType> findFirstByCode(String code);
}
