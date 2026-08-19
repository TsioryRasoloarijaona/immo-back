package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspectionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComProspectionTypeRepository extends JpaRepository<ComProspectionType, UUID> {

    List<ComProspectionType> findByComProspectionId(UUID prospectionId);

    void deleteByIdAndComProspectionId(UUID id, UUID prospectionId);
}
