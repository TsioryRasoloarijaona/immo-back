package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlQuote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StlQuoteRepository extends JpaRepository<StlQuote, UUID> {

    Page<StlQuote> findByComProspectionIdOrderByDocDateDesc(UUID prospectionId, Pageable pageable);
}
