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
     * Sum of a {@code StlSettlement}'s amount already affected to (any) échéance(s) — used by
     * {@code GET /api/encaissements} to compute "Montant brut restant" ({@code Amount} minus this).
     */
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM StlSettlementLine l WHERE l.stlSettlement.id = :settlementId")
    BigDecimal sumAmountByStlSettlementId(UUID settlementId);

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
    @Query("SELECT f.cfgCustomer.id, COALESCE(SUM(l.amount), 0) FROM StlSettlementLine l " +
            "JOIN l.comDocument d JOIN d.comFolder f " +
            "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc " +
            "WHERE f.cfgCustomer.id IN :clientIds AND (soc.isActive = true OR soc.id IS NULL) " +
            "GROUP BY f.cfgCustomer.id")
    List<Object[]> sumAmountGroupedByClientId(List<UUID> clientIds);

    /**
     * Tous les règlements de ce dossier, triés par date de règlement (StlSettlement.docDate)
     * croissante — c'est le tri utilisé par le tableau "FolderLines" de la Fiche Trop Perçu
     * legacy (ComSaleFicheTropPercu.yes), contrairement à findByDossierIdOrderByEcheanceDateAsc
     * qui trie par date d'échéance.
     */
    @Query("SELECT l FROM StlSettlementLine l JOIN l.comDocument d " +
            "WHERE d.comFolder.id = :dossierId " +
            "ORDER BY l.stlSettlement.docDate ASC")
    List<StlSettlementLine> findByDossierIdOrderByStlSettlementDocDateAsc(UUID dossierId);

    /**
     * Historique complet des règlements d'une seule échéance ({@code ComDocument}), plus récent
     * d'abord — backs {@code GET /api/dossiers/{id}/echeancier/{echeanceId}/reglements}.
     */
    List<StlSettlementLine> findByComDocumentIdOrderByStlSettlement_DocDateDesc(UUID echeanceId);

    /**
     * La/les ligne(s) rattachant un {@code StlSettlement} donné à ce Dossier précis — un même
     * règlement peut en théorie couvrir plusieurs échéances/dossiers, d'où une liste plutôt qu'un
     * singleton. Utilisé par {@code RecuReglementService} pour retrouver le montant réglé
     * spécifique à ce dossier ({@code StlSettlementLine.Amount}) — jamais {@code StlSettlement.Amount}
     * qui peut couvrir d'autres dossiers.
     */
    @Query("SELECT l FROM StlSettlementLine l JOIN l.comDocument d " +
            "WHERE l.stlSettlement.id = :settlementId AND d.comFolder.id = :dossierId")
    List<StlSettlementLine> findBySettlementIdAndDossierId(UUID settlementId, UUID dossierId);
}
