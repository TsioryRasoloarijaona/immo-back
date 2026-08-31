package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlQuoteItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StlQuoteItemRepository extends JpaRepository<StlQuoteItem, UUID> {

    List<StlQuoteItem> findByStlQuoteIdOrderByCodeAsc(UUID stlQuoteId);
}
