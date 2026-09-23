package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspectionKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ComProspectionKindRepository extends JpaRepository<ComProspectionKind, UUID> {

    /** Par code métier — sert les valeurs par défaut de l'import CSV des leads. */
    Optional<ComProspectionKind> findFirstByCode(String code);
}
