package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspectionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComProspectionCategoryRepository extends JpaRepository<ComProspectionCategory, UUID> {

    List<ComProspectionCategory> findByComProspectionId(UUID prospectionId);

    void deleteByIdAndComProspectionId(UUID id, UUID prospectionId);
}
