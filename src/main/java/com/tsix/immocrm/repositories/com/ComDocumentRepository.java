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

    /**
     * Bloc A de {@code GET /api/dashboard/annuel} — l'échéancier de l'année, agrégé par mois.
     * Tuple : [mois (1-12), somme des montants, nombre d'échéances].
     *
     * <p><b>Ce n'est pas un encaissé.</b> {@code ComDocument.Amount} est une somme <em>due</em> ;
     * des mois à venir sont donc renseignés. Le nom du champ servi le dit
     * ({@code echeancier}), pas {@code realise}.
     *
     * <p>Les catégories retenues sont avance, crédit et autofinancement, <b>frais exclus</b>.
     * La requête du légataire écrit ici {@code (… OR IsSaleExpense=1) AND IsSaleExpense=0}, deux
     * clauses qui se contredisent : l'effet net est « frais exclus » et c'est cette intention qui
     * est reprise, pas la contradiction.
     *
     * <p>Le seuil de concrétisation et les statuts de sortie sont passés par l'appelant plutôt que
     * figés : voir {@code SyntheseAnnuelleService} pour leur résolution.
     *
     * <p>La somme est rendue <b>telle quelle, éventuellement {@code null}</b> — aucun
     * {@code COALESCE(..., 0)} en SQL : le littéral entier y dégrade silencieusement l'échelle
     * d'une somme d'argent. La substitution se fait en Java, en {@code BigDecimal}.
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
