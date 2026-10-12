package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

public interface StlSettlementLineRepository extends JpaRepository<StlSettlementLine, UUID> {

    /** Toutes les sommes de lignes excluent les règlements annulés (FEATURE-lot4 §2). */
    String COMPTE = " AND " + StatutReglement.JPQL_COMPTE_DANS_LES_TOTAUX;

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM StlSettlementLine l JOIN l.stlSettlement st "
            + "WHERE l.comDocument.id = :echeanceId" + COMPTE)
    BigDecimal sumAmountByComDocumentId(UUID echeanceId);

    /** Part affectée d'un règlement, retranchée de {@code Amount} pour le « Montant brut restant ». */
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM StlSettlementLine l JOIN l.stlSettlement st "
            + "WHERE st.id = :settlementId" + COMPTE)
    BigDecimal sumAmountByStlSettlementId(UUID settlementId);

    /** Tuple : [comDocumentId, sommeReglee] ; seules les échéances avec un règlement apparaissent. */
    @Query("SELECT l.comDocument.id, COALESCE(SUM(l.amount), 0) FROM StlSettlementLine l JOIN l.stlSettlement st " +
            "WHERE l.comDocument.comFolder.id = :dossierId" + COMPTE + " GROUP BY l.comDocument.id")
    List<Object[]> sumAmountByComFolderIdGroupedByComDocument(UUID dossierId);

    /** Dossier d'un règlement par ses lignes d'échéance. Tuple : [comFolderId, comFolderCode]. */
    @Query("SELECT d.comFolder.id, d.comFolder.code FROM StlSettlementLine l JOIN l.comDocument d " +
            "WHERE l.stlSettlement.id = :settlementId AND d.comFolder IS NOT NULL")
    List<Object[]> findDossierBySettlementId(UUID settlementId);

    /** Règlements des échéances « AVANCE », pour le mode de paiement de la fiche de réservation. */
    @Query("SELECT l FROM StlSettlementLine l JOIN l.comDocument d " +
            "WHERE d.comFolder.id = :dossierId AND d.comSettlementCategory.isSaleAdvance = true " +
            "ORDER BY l.stlSettlement.settlementDate ASC, l.stlSettlement.docDate ASC")
    List<StlSettlementLine> findAvanceLinesByDossierIdOrderByDateAsc(UUID dossierId);

    /** Repli de la fiche de réservation sans ligne « AVANCE » (dossier tout en auto-financement). */
    @Query("SELECT l FROM StlSettlementLine l JOIN l.comDocument d " +
            "WHERE d.comFolder.id = :dossierId " +
            "ORDER BY d.docDate ASC, l.stlSettlement.settlementDate ASC")
    List<StlSettlementLine> findByDossierIdOrderByEcheanceDateAsc(UUID dossierId);

    /**
     * Tuple : [cfgCustomerId, montantRegle]. Le {@code COALESCE(…, 0)} à littéral entier peut arrondir
     * la somme : défaut connu, non corrigé (FEATURE-commentaires-et-docs-api.md, défauts n° 9).
     */
    @Query("SELECT f.cfgCustomer.id, COALESCE(SUM(l.amount), 0) FROM StlSettlementLine l " +
            "JOIN l.stlSettlement st JOIN l.comDocument d JOIN d.comFolder f " +
            "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc " +
            "WHERE f.cfgCustomer.id IN :clientIds AND (soc.isActive = true OR soc.id IS NULL)" + COMPTE +
            " GROUP BY f.cfgCustomer.id")
    List<Object[]> sumAmountGroupedByClientId(List<UUID> clientIds);

    /** Tri par date du règlement, celui du tableau "FolderLines" de la Fiche Trop Perçu du légataire. */
    @Query("SELECT l FROM StlSettlementLine l JOIN l.comDocument d " +
            "WHERE d.comFolder.id = :dossierId " +
            "ORDER BY l.stlSettlement.docDate ASC")
    List<StlSettlementLine> findByDossierIdOrderByStlSettlementDocDateAsc(UUID dossierId);

    List<StlSettlementLine> findByComDocumentIdOrderByStlSettlement_DocDateDesc(UUID echeanceId);

    /** Tuple : [buyDocumentId, réglé] — factures fournisseur (module Chantier), règlements annulés exclus. */
    @Query("SELECT l.buyDocumentId, SUM(l.amount) FROM StlSettlementLine l JOIN l.stlSettlement st "
            + "WHERE l.buyDocumentId IN :factureIds" + COMPTE + " GROUP BY l.buyDocumentId")
    List<Object[]> sumAmountByBuyDocumentIds(Collection<UUID> factureIds);

    /** Toutes les lignes d'une facture fournisseur, annulées comprises (la fiche les affiche barrées). */
    @Query("SELECT l FROM StlSettlementLine l JOIN FETCH l.stlSettlement st LEFT JOIN FETCH st.stlSettlementType "
            + "WHERE l.buyDocumentId = :factureId ORDER BY st.docDate ASC, st.code ASC")
    List<StlSettlementLine> findByBuyDocumentId(UUID factureId);

    /**
     * Lignes d'un règlement sur ce dossier : le montant réglé pour le dossier est la somme de leurs
     * {@code Amount}, jamais {@code StlSettlement.Amount}, qui peut couvrir d'autres dossiers.
     */
    @Query("SELECT l FROM StlSettlementLine l JOIN l.comDocument d " +
            "WHERE l.stlSettlement.id = :settlementId AND d.comFolder.id = :dossierId")
    List<StlSettlementLine> findBySettlementIdAndDossierId(UUID settlementId, UUID dossierId);

    /**
     * Montant croissant, l'ordre de l'écran légataire (trace d'audit). {@code LEFT JOIN FETCH} : une
     * ligne sans échéance ou sans dossier doit apparaître incomplète, pas disparaître.
     */
    @Query("SELECT l FROM StlSettlementLine l "
            + "LEFT JOIN FETCH l.comDocument d "
            + "LEFT JOIN FETCH d.comFolder "
            + "LEFT JOIN FETCH d.comSettlementCategory "
            + "WHERE l.stlSettlement.id = :settlementId "
            + "ORDER BY l.amount ASC")
    List<StlSettlementLine> findAffectationsBySettlementId(UUID settlementId);

    /**
     * Bloc B : réglé sur les ventes concrétisées, {@code null} si rien. Signé comme le légataire : une
     * catégorie pas au crédit compte en négatif ; avance, crédit, autofinancement seulement.
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
            + "AND (soc.isActive = true OR soc.id IS NULL)" + COMPTE)
    BigDecimal montantReglePourVentes(LocalDateTime debut, LocalDateTime fin, int seuil,
                                      List<String> sorties);

    /** Filtre par tranche : {@code dossierIds} = assiette de {@code /api/dossiers?trancheId=}, jamais vide. */
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
            + "AND f.id IN :dossierIds" + COMPTE)
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
            + "AND (pr.id IN :projetIds OR pr.id IS NULL) AND (soc.isActive = true OR soc.id IS NULL)" + COMPTE)
    BigDecimal montantReglePourVentesDansPerimetre(LocalDateTime debut, LocalDateTime fin, int seuil,
                                                    List<String> sorties, List<UUID> projetIds);

    /**
     * Bloc C : [nombre, somme] par date de règlement si pointé, d'échéance sinon. Jointure
     * {@code StkItem ON ComFolderId … IsUnderItem = 0} du légataire omise exprès : elle doublerait les
     * dossiers à deux articles principaux et exclurait ceux sans article.
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
            + "AND (soc.isActive = true OR soc.id IS NULL)" + COMPTE)
    List<Object[]> encaissementsDeLAnnee(LocalDateTime debut, LocalDateTime fin);

    /** Filtre par tranche : {@code dossierIds} = assiette de {@code /api/dossiers?trancheId=}, jamais vide. */
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
            + "AND f.id IN :dossierIds" + COMPTE)
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
            + "AND (pr.id IN :projetIds OR pr.id IS NULL) AND (soc.isActive = true OR soc.id IS NULL)" + COMPTE)
    List<Object[]> encaissementsDeLAnneeDansPerimetre(LocalDateTime debut, LocalDateTime fin,
                                                       List<UUID> projetIds);
}
