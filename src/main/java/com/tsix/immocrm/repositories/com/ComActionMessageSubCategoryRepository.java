package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComActionMessageSubCategoryRepository extends JpaRepository<ComActionMessageSubCategory, UUID> {

    /**
     * {@code GET /api/referentiels/resultats-interaction} — the "Résultat" dropdown for a
     * hotline/prospection interaction: every sub-category under the
     * {@code ComActionMessageCategory} coded {@code categoryCode} (confirmed empirically:
     * {@code "Prospection"} — 23 rows in real data).
     */
    List<ComActionMessageSubCategory> findByComActionMessageCategoryCodeOrderByDescriptionAsc(String categoryCode);
}
