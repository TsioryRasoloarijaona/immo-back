package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComActionMessageSubCategoryRepository extends JpaRepository<ComActionMessageSubCategory, UUID> {

    /** Liste « Résultat » hotline/prospection : {@code categoryCode = "Prospection"}. */
    List<ComActionMessageSubCategory> findByComActionMessageCategoryCodeOrderByDescriptionAsc(String categoryCode);
}
