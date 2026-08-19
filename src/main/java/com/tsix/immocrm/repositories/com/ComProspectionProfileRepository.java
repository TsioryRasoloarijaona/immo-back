package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspectionProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComProspectionProfileRepository extends JpaRepository<ComProspectionProfile, UUID> {
    List<ComProspectionProfile> findByComProspectionProfileCategoryIdOrderBySortingAsc(UUID categoryId);
}
