package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComActionMessageSubCategoryRepository extends JpaRepository<ComActionMessageSubCategory, UUID> {

    /** Liste « Résultat » hotline/prospection : {@code categoryCode = "Prospection"}. */
    List<ComActionMessageSubCategory> findByComActionMessageCategoryCodeOrderByDescriptionAsc(String categoryCode);

    /** Un même code existe dans plusieurs catégories (« Téléphone constamment occupé ») : la catégorie départage. */
    Optional<ComActionMessageSubCategory> findFirstByCodeAndComActionMessageCategoryId(String code, UUID categorieId);
}
