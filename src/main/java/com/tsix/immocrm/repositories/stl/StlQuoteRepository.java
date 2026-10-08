package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlQuote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface StlQuoteRepository extends JpaRepository<StlQuote, UUID>, JpaSpecificationExecutor<StlQuote> {

    Page<StlQuote> findByComProspectionIdOrderByDocDateDesc(UUID prospectionId, Pageable pageable);

    /** Pour réexpédier l'offre existante plutôt qu'en créer une seconde (FEATURE-offre-apres-visite.md §4). */
    Optional<StlQuote> findFirstByComProspectionIdOrderByDocDateDesc(UUID prospectionId);
}
