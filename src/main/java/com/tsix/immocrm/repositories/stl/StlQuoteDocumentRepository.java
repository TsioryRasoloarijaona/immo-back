package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlQuoteDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StlQuoteDocumentRepository extends JpaRepository<StlQuoteDocument, UUID> {

    List<StlQuoteDocument> findByStlQuoteIdOrderByCodeAsc(UUID stlQuoteId);
}
