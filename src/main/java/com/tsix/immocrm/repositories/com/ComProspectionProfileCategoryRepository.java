package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspectionProfileCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComProspectionProfileCategoryRepository extends JpaRepository<ComProspectionProfileCategory, UUID> {
    List<ComProspectionProfileCategory> findAllByOrderBySortingAsc();
}
