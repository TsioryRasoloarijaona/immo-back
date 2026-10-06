package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlSettlementLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

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

    /**
     * Toutes les affectations d'un règlement — les échéances qu'il solde — <strong>montant
     * croissant</strong>, l'ordre de l'écran légataire ({@code ORDER BY Amount ASC}, relevé dans
     * la trace d'audit du 14/09/2026 à l'ouverture d'une fiche règlement).
     *
     * <p>Les trois associations sont chargées en {@code JOIN FETCH} : la fiche les déréférence
     * toutes, et sans cela une ligne coûterait trois requêtes de plus. Toutes en {@code LEFT} —
     * une ligne sans échéance, ou une échéance sans dossier, ne doit pas disparaître de la fiche,
     * elle doit y apparaître incomplète.
     *
     * <p>Backs {@code GET /api/encaissements/{id}}.
     */
    @Query("SELECT l FROM StlSettlementLine l "
            + "LEFT JOIN FETCH l.comDocument d "
            + "LEFT JOIN FETCH d.comFolder "
            + "LEFT JOIN FETCH d.comSettlementCategory "
            + "WHERE l.stlSettlement.id = :settlementId "
            + "ORDER BY l.amount ASC")
    List<StlSettlementLine> findAffectationsBySettlementId(UUID settlementId);

    /**
     * Bloc B de {@code GET /api/dashboard/annuel} — ce qui a déjà été réglé sur les ventes
     * concrétisées de l'année. Rend {@code null} quand rien n'a été réglé.
     *
     * <p>Le montant est <b>signé</b> par le sens de la catégorie, comme la sous-requête corrélée
     * du légataire : une catégorie qui n'est pas au crédit compte en négatif. Seuls l'avance, le
     * crédit et l'autofinancement entrent — pas les frais.
     */
    @Query("SELECT SUM(CASE WHEN cat.isCredit = true THEN l.amount ELSE -l.amount END) "
            + "FROM StlSettlementLine l JOIN l.comDocument d JOIN d.comSettlementCategory cat "
            + "JOIN d.comFolder f JOIN f.strStatus s JOIN l.stlSettlement st "
            + "JOIN st.stlSettlementType sty "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.advanceDate >= :debut AND f.advanceDate < :fin "
            + "AND s.sorting >= :seuil AND s.code NOT IN :sorties "
            + "AND (sty.isNotContabilized IS NULL OR sty.isNotContabilized = false) "
            + "AND (cat.isSaleAdvance = true OR cat.isSaleCredit = true OR cat.isSaleAutoFinancment = true) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    BigDecimal montantReglePourVentes(LocalDateTime debut, LocalDateTime fin, int seuil,
                                      List<String> sorties);

    /**
     * Variante de {@link #montantReglePourVentes} restreinte à une liste de dossiers — même tuple. Sert le
     * filtre par tranche du tableau de bord : {@code dossierIds} est l'assiette de
     * {@code GET /api/dossiers?trancheId=…} (périmètre commercial compris), jamais vide.
     */
    @Query("SELECT SUM(CASE WHEN cat.isCredit = true THEN l.amount ELSE -l.amount END) "
            + "FROM StlSettlementLine l JOIN l.comDocument d JOIN d.comSettlementCategory cat "
            + "JOIN d.comFolder f JOIN f.strStatus s JOIN l.stlSettlement st "
            + "JOIN st.stlSettlementType sty "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.advanceDate >= :debut AND f.advanceDate < :fin "
            + "AND s.sorting >= :seuil AND s.code NOT IN :sorties "
            + "AND (sty.isNotContabilized IS NULL OR sty.isNotContabilized = false) "
            + "AND (cat.isSaleAdvance = true OR cat.isSaleCredit = true OR cat.isSaleAutoFinancment = true) "
            + "AND (soc.isActive = true OR soc.id IS NULL) "
            + "AND f.id IN :dossierIds")
    BigDecimal montantReglePourVentesPourDossiers(LocalDateTime debut, LocalDateTime fin, int seuil,
                                      List<String> sorties, List<UUID> dossierIds);

    /** Variante de {@link #montantReglePourVentes} restreinte au périmètre. */
    @Query("SELECT SUM(CASE WHEN cat.isCredit = true THEN l.amount ELSE -l.amount END) "
            + "FROM StlSettlementLine l JOIN l.comDocument d JOIN d.comSettlementCategory cat "
            + "JOIN d.comFolder f JOIN f.strStatus s JOIN l.stlSettlement st "
            + "JOIN st.stlSettlementType sty "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.advanceDate >= :debut AND f.advanceDate < :fin "
            + "AND s.sorting >= :seuil AND s.code NOT IN :sorties "
            + "AND (sty.isNotContabilized IS NULL OR sty.isNotContabilized = false) "
            + "AND (cat.isSaleAdvance = true OR cat.isSaleCredit = true OR cat.isSaleAutoFinancment = true) "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL) AND (soc.isActive = true OR soc.id IS NULL)")
    BigDecimal montantReglePourVentesDansPerimetre(LocalDateTime debut, LocalDateTime fin, int seuil,
                                                    List<String> sorties, List<UUID> projetIds);

    /**
     * Bloc C de {@code GET /api/dashboard/annuel} — les encaissements de l'année.
     * Tuple : [nombre de lignes, somme].
     *
     * <p>L'année est celle de la <b>date de règlement si le règlement est pointé, de l'échéance
     * sinon</b> : {@code SettlementDate} n'est renseignée que sur les pointés (14 lignes réelles
     * sur 3 244), le reste tombe sur {@code EcheanceDate}.
     *
     * <p><b>Une jointure du légataire est volontairement absente.</b> Sa requête joint
     * {@code StkItem ON ComFolder.PKey = StkItem.ComFolderId} avec {@code IsUnderItem = 0}. Cette
     * jointure ne change rien aujourd'hui — mesuré : 82 lignes et 7 623 785 DH avec comme sans —
     * mais elle <em>double</em> les lignes des 3 dossiers portant deux articles principaux et
     * <em>exclut</em> les 37 qui n'en portent aucun. La reprendre importerait une
     * double-comptabilisation latente pour zéro gain.
     */
    @Query("SELECT COUNT(l), SUM(l.amount) "
            + "FROM StlSettlementLine l JOIN l.stlSettlement st JOIN st.stlSettlementType sty "
            + "JOIN l.comDocument d JOIN d.comSettlementCategory cat JOIN d.comFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE st.isCredit = true "
            + "AND (sty.isNotContabilized IS NULL OR sty.isNotContabilized = false) "
            + "AND (cat.isSaleExpense IS NULL OR cat.isSaleExpense = false) "
            + "AND COALESCE(st.settlementDate, st.echeanceDate) >= :debut "
            + "AND COALESCE(st.settlementDate, st.echeanceDate) < :fin "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    List<Object[]> encaissementsDeLAnnee(LocalDateTime debut, LocalDateTime fin);

    /**
     * Variante de {@link #encaissementsDeLAnnee} restreinte à une liste de dossiers — même tuple. Sert le
     * filtre par tranche du tableau de bord : {@code dossierIds} est l'assiette de
     * {@code GET /api/dossiers?trancheId=…} (périmètre commercial compris), jamais vide.
     */
    @Query("SELECT COUNT(l), SUM(l.amount) "
            + "FROM StlSettlementLine l JOIN l.stlSettlement st JOIN st.stlSettlementType sty "
            + "JOIN l.comDocument d JOIN d.comSettlementCategory cat JOIN d.comFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE st.isCredit = true "
            + "AND (sty.isNotContabilized IS NULL OR sty.isNotContabilized = false) "
            + "AND (cat.isSaleExpense IS NULL OR cat.isSaleExpense = false) "
            + "AND COALESCE(st.settlementDate, st.echeanceDate) >= :debut "
            + "AND COALESCE(st.settlementDate, st.echeanceDate) < :fin "
            + "AND (soc.isActive = true OR soc.id IS NULL) "
            + "AND f.id IN :dossierIds")
    List<Object[]> encaissementsDeLAnneePourDossiers(LocalDateTime debut, LocalDateTime fin, List<UUID> dossierIds);

    /** Variante de {@link #encaissementsDeLAnnee} restreinte au périmètre — même tuple. */
    @Query("SELECT COUNT(l), SUM(l.amount) "
            + "FROM StlSettlementLine l JOIN l.stlSettlement st JOIN st.stlSettlementType sty "
            + "JOIN l.comDocument d JOIN d.comSettlementCategory cat JOIN d.comFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE st.isCredit = true "
            + "AND (sty.isNotContabilized IS NULL OR sty.isNotContabilized = false) "
            + "AND (cat.isSaleExpense IS NULL OR cat.isSaleExpense = false) "
            + "AND COALESCE(st.settlementDate, st.echeanceDate) >= :debut "
            + "AND COALESCE(st.settlementDate, st.echeanceDate) < :fin "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL) AND (soc.isActive = true OR soc.id IS NULL)")
    List<Object[]> encaissementsDeLAnneeDansPerimetre(LocalDateTime debut, LocalDateTime fin,
                                                       List<UUID> projetIds);
}
