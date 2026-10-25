package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmSpecificFieldEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdmSpecificFieldEntityRepository extends JpaRepository<AdmSpecificFieldEntity, UUID> {

    /** OMRAN peut avoir plusieurs lignes du même code : la première suffit pour rattacher une définition. */
    Optional<AdmSpecificFieldEntity> findFirstByCodeOrderByIdAsc(String code);
}
