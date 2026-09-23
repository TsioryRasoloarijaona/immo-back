package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComProspectionOriginRepository extends JpaRepository<ComProspectionOrigin, UUID> {

    List<ComProspectionOrigin> findAllByOrderBySortingAsc();

    /** Par code métier — sert les valeurs par défaut de l'import CSV des leads. */
    Optional<ComProspectionOrigin> findFirstByCode(String code);
}
