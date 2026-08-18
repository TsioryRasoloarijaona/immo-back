package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StkItemCategoryRepository extends JpaRepository<StkItemCategory, UUID> {
}
