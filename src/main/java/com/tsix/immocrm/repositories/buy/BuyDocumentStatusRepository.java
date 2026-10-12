package com.tsix.immocrm.repositories.buy;

import com.tsix.immocrm.entities.buy.BuyDocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BuyDocumentStatusRepository extends JpaRepository<BuyDocumentStatus, UUID> {
}
