package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ComDocumentRepository extends JpaRepository<ComDocument, UUID> {

    // Full, unpaged list — used for server-side aggregation (GET /api/dossiers/{id}/montants),
    // where every line must be summed, not just one page of them. Order is irrelevant for a sum.
    List<ComDocument> findByComFolderIdOrderByDocDateAsc(UUID comFolderId);

    // GET /api/dossiers/{id}/echeancier — most recent échéance first.
    Page<ComDocument> findByComFolderIdOrderByDocDateDesc(UUID comFolderId, Pageable pageable);

    /**
     * Total "à payer" per client (sum of every échéance amount across all of their dossiers) —
     * used by {@code GET /api/clients} (server-side aggregate, one round trip for every client
     * on the page). Tuple: [cfgCustomerId, montantTotal].
     */
    @Query("SELECT f.cfgCustomer.id, COALESCE(SUM(d.amount), 0) FROM ComDocument d JOIN d.comFolder f " +
            "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc " +
            "WHERE f.cfgCustomer.id IN :clientIds AND (soc.isActive = true OR soc.id IS NULL) " +
            "GROUP BY f.cfgCustomer.id")
    List<Object[]> sumAmountGroupedByClientId(List<UUID> clientIds);
}
