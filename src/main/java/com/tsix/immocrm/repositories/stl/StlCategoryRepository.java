package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StlCategoryRepository extends JpaRepository<StlCategory, UUID> {
}
