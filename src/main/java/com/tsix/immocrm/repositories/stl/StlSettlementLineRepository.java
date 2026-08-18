package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlSettlementLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface StlSettlementLineRepository extends JpaRepository<StlSettlementLine, UUID> {

    /** Sum of all règlement amounts posted against a single échéance ({@code ComDocument}). */
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM StlSettlementLine l WHERE l.comDocument.id = :echeanceId")
    BigDecimal sumAmountByComDocumentId(UUID echeanceId);

    /**
     * Per-échéance règlement totals for every échéance of a Dossier, in one round trip.
     * Tuple: [comDocumentId, sommeReglee]. Only échéances with at least one règlement appear.
     */
    @Query("SELECT l.comDocument.id, COALESCE(SUM(l.amount), 0) FROM StlSettlementLine l " +
            "WHERE l.comDocument.comFolder.id = :dossierId GROUP BY l.comDocument.id")
    List<Object[]> sumAmountByComFolderIdGroupedByComDocument(UUID dossierId);
}
