package com.tsix.immocrm.repositories.buy;

import com.tsix.immocrm.entities.buy.BuyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BuyDocumentRepository extends JpaRepository<BuyDocument, UUID> {

    /** Aucun index unique en base : « une facture par frais » est tenu par {@code FraisFactures}. */
    List<BuyDocument> findByStkExpenseIdIn(Collection<UUID> fraisIds);
}
