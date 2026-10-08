package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ComDocumentRepository extends JpaRepository<ComDocument, UUID> {

    // Non paginée : les montants somment toutes les lignes, pas une page.
    List<ComDocument> findByComFolderIdOrderByDocDateAsc(UUID comFolderId);

    Page<ComDocument> findByComFolderIdOrderByDocDateDesc(UUID comFolderId, Pageable pageable);

    /**
     * Tuple : [cfgCustomerId, total des échéances]. Le {@code COALESCE(…, 0)} à littéral entier peut
     * arrondir la somme : défaut connu, non corrigé (FEATURE-commentaires-et-docs-api.md, défauts n° 9).
     */
    @Query("SELECT f.cfgCustomer.id, COALESCE(SUM(d.amount), 0) FROM ComDocument d JOIN d.comFolder f " +
            "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc " +
            "WHERE f.cfgCustomer.id IN :clientIds AND (soc.isActive = true OR soc.id IS NULL) " +
            "GROUP BY f.cfgCustomer.id")
    List<Object[]> sumAmountGroupedByClientId(List<UUID> clientIds);

    /**
     * Bloc A de {@code /api/dashboard/annuel} : [mois, somme, nombre] des sommes <em>dues</em>, pas encaissées.
     * Frais exclus : le légataire écrit {@code (… OR IsSaleExpense=1) AND IsSaleExpense=0}, on reprend l'effet
     * net. Somme {@code null} possible : pas de {@code COALESCE} entier en SQL, le zéro se substitue en Java.
     */
    @Query("SELECT MONTH(d.docDate), SUM(d.amount), COUNT(d) "
            + "FROM ComDocument d JOIN d.comFolder f JOIN f.strStatus s "
            + "JOIN d.comSettlementCategory cat " + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE YEAR(d.docDate) = :annee AND s.sorting >= :seuil AND s.code NOT IN :sorties "
            + "AND (cat.isSaleAdvance = true OR cat.isSaleCredit = true OR cat.isSaleAutoFinancment = true) "
            + "AND (cat.isSaleExpense IS NULL OR cat.isSaleExpense = false) "
            + "AND (soc.isActive = true OR soc.id IS NULL) "
            + "GROUP BY MONTH(d.docDate)")
    List<Object[]> echeancierParMois(int annee, int seuil, List<String> sorties);

    /** Filtre par tranche : {@code dossierIds} = assiette de {@code /api/dossiers?trancheId=}, jamais vide. */
    @Query("SELECT MONTH(d.docDate), SUM(d.amount), COUNT(d) "
            + "FROM ComDocument d JOIN d.comFolder f JOIN f.strStatus s "
            + "JOIN d.comSettlementCategory cat " + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE YEAR(d.docDate) = :annee AND s.sorting >= :seuil AND s.code NOT IN :sorties "
            + "AND (cat.isSaleAdvance = true OR cat.isSaleCredit = true OR cat.isSaleAutoFinancment = true) "
            + "AND (cat.isSaleExpense IS NULL OR cat.isSaleExpense = false) "
            + "AND (soc.isActive = true OR soc.id IS NULL) "
            + "AND f.id IN :dossierIds "
            + "GROUP BY MONTH(d.docDate)")
    List<Object[]> echeancierParMoisPourDossiers(int annee, int seuil, List<String> sorties, List<UUID> dossierIds);

    /** Variante de {@link #echeancierParMois} restreinte au périmètre — même tuple. */
    @Query("SELECT MONTH(d.docDate), SUM(d.amount), COUNT(d) "
            + "FROM ComDocument d JOIN d.comFolder f JOIN f.strStatus s "
            + "JOIN d.comSettlementCategory cat " + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE YEAR(d.docDate) = :annee AND s.sorting >= :seuil AND s.code NOT IN :sorties "
            + "AND (cat.isSaleAdvance = true OR cat.isSaleCredit = true OR cat.isSaleAutoFinancment = true) "
            + "AND (cat.isSaleExpense IS NULL OR cat.isSaleExpense = false) "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL) AND (soc.isActive = true OR soc.id IS NULL) "
            + "GROUP BY MONTH(d.docDate)")
    List<Object[]> echeancierParMoisDansPerimetre(int annee, int seuil, List<String> sorties,
                                                  List<UUID> projetIds);
}
