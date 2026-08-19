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

    /**
     * Resolves which Dossier a règlement belongs to, via its first échéance line — used by
     * {@code GET /api/prospects/{id}/reglements} (GAP-06) to surface dossierId/dossierCode.
     * Tuple: [comFolderId, comFolderCode].
     */
    @Query("SELECT d.comFolder.id, d.comFolder.code FROM StlSettlementLine l JOIN l.comDocument d " +
            "WHERE l.stlSettlement.id = :settlementId AND d.comFolder IS NOT NULL")
    List<Object[]> findDossierBySettlementId(UUID settlementId);

    /**
     * Règlement(s) tied to this Dossier's "AVANCE" (réservation) échéance(s), oldest first — used
     * to source the "Mode de paiement" / "Montant" / "Réglé par" fields on the Fiche de
     * réservation. See {@code ComSettlementCategory.isSaleAdvance}.
     */
    @Query("SELECT l FROM StlSettlementLine l JOIN l.comDocument d " +
            "WHERE d.comFolder.id = :dossierId AND d.comSettlementCategory.isSaleAdvance = true " +
            "ORDER BY l.stlSettlement.settlementDate ASC, l.stlSettlement.docDate ASC")
    List<StlSettlementLine> findAvanceLinesByDossierIdOrderByDateAsc(UUID dossierId);

    /**
     * All règlements of this Dossier, oldest first (by échéance date) — fallback for the Fiche de
     * réservation when no "AVANCE" line exists (e.g. dossier paid 100% auto-financement at
     * réservation time, as confirmed on real data).
     */
    @Query("SELECT l FROM StlSettlementLine l JOIN l.comDocument d " +
            "WHERE d.comFolder.id = :dossierId " +
            "ORDER BY d.docDate ASC, l.stlSettlement.settlementDate ASC")
    List<StlSettlementLine> findByDossierIdOrderByEcheanceDateAsc(UUID dossierId);

    /**
     * Total "réglé" per client (sum of every règlement across all of their dossiers) — used by
     * {@code GET /api/clients} (server-side aggregate). Tuple: [cfgCustomerId, montantRegle].
     */
    @Query("SELECT d.comFolder.cfgCustomer.id, COALESCE(SUM(l.amount), 0) FROM StlSettlementLine l JOIN l.comDocument d " +
            "WHERE d.comFolder.cfgCustomer.id IN :clientIds GROUP BY d.comFolder.cfgCustomer.id")
    List<Object[]> sumAmountGroupedByClientId(List<UUID> clientIds);
}
