package com.tsix.immocrm.repositories.buy;

import com.tsix.immocrm.entities.buy.BuyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BuyDocumentRepository extends JpaRepository<BuyDocument, UUID> {
}
