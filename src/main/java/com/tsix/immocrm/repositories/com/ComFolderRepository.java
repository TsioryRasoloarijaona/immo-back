package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComFolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ComFolderRepository extends JpaRepository<ComFolder, UUID>, JpaSpecificationExecutor<ComFolder> {

    /**
     * {@code GET /api/dossiers} — surcharge la méthode de {@link JpaSpecificationExecutor} pour
     * charger en une seule requête les quatre associations que
     * {@code DossierService.toListItem} déréférence sur <b>chaque</b> ligne.
     *
     * <p><b>Sans ce graphe, la liste est un N+1.</b> Les quatre associations sont
     * {@code @ManyToOne(fetch = LAZY)} : une page de 20 déclenchait jusqu'à 80 SELECT
     * supplémentaires, et le temps de réponse était strictement proportionnel à la taille de
     * page (mesuré : 0,72 s à {@code size=1}, 3,51 s à 20, 7,72 s à 50 — soit ~0,15 s par ligne,
     * dont l'essentiel en allers-retours réseau). Le volume n'y était pour rien : 358 dossiers
     * en tout.
     *
     * <p>Les quatre sont des {@code ToOne} : le join-fetch reste compatible avec la pagination,
     * qui continue de se faire en SQL. Un {@code @EntityGraph} sur une collection provoquerait au
     * contraire une pagination en mémoire — à ne pas ajouter ici.
     */
    @Override
    @EntityGraph(attributePaths = {"strStatus", "cfgCustomer", "cfgCommercial", "comProspection"})
    Page<ComFolder> findAll(Specification<ComFolder> spec, Pageable pageable);

    /**
     * Lightweight projection for {@code GET /api/dashboard} — avoids loading full ComFolder
     * rows (100+ columns) for an aggregate. Tuple: [id, statutCode, totalToPay, revival1,
     * revival1ValidityDate, revival2, revival2ValidityDate, customerSocialReason,
     * customerFirstName, customerLastName].
     */
    @Query("SELECT f.id, s.code, f.totalToPay, f.revival1, f.revival1ValidityDate, f.revival2, f.revival2ValidityDate, " +
            "c.socialReason, c.firstName, c.lastName " +
            "FROM ComFolder f LEFT JOIN f.strStatus s LEFT JOIN f.cfgCustomer c " +
            "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc " +
            "WHERE (soc.isActive = true OR soc.id IS NULL)")
    List<Object[]> findDashboardProjection();

    /**
     * Variante de {@link #findDashboardProjection()} restreinte au périmètre commercial de
     * l'appelant — même tuple, mêmes colonnes. Le projet d'un dossier se résout obligatoirement
     * par la tranche de son article principal ({@code ComFolder.StkItemId} →
     * {@code StkItem.StkHierarchyId} → {@code CfgTranche.CfgProjectId}) : le raccourci
     * {@code StkItem.CfgProjectId} existe mais est mort en base réelle (0 ligne sur 1068).
     * <p>
     * Les jointures sont volontairement en {@code LEFT JOIN} et le prédicat accepte
     * {@code pr.id IS NULL} : un dossier dont le projet ne se résout pas reste visible de tous
     * (§6.4 — le masquer signifierait que plus personne ne le traite). Une jointure interne le
     * masquerait silencieusement.
     */
    @Query("SELECT f.id, s.code, f.totalToPay, f.revival1, f.revival1ValidityDate, f.revival2, f.revival2ValidityDate, " +
            "c.socialReason, c.firstName, c.lastName " +
            "FROM ComFolder f LEFT JOIN f.strStatus s LEFT JOIN f.cfgCustomer c " +
            "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc " +
            "WHERE (pr.id IN :projetIds OR pr.id IS NULL) AND (soc.isActive = true OR soc.id IS NULL)")
    List<Object[]> findDashboardProjectionDansPerimetre(@Param("projetIds") List<UUID> projetIds);

    /**
     * Point de contrôle unique du périmètre sur le chargement d'un dossier par id (voir
     * {@code PerimetreInterceptor}) : le dossier existe-t-il ET est-il dans le périmètre ?
     * Renvoie aussi {@code true} pour un dossier dont le projet ne se résout pas (§6.4).
     * Un dossier hors périmètre est traité exactement comme un dossier inexistant — 404, jamais
     * 403, sinon l'existence de la ressource serait confirmée et les ids énumérables.
     */
    @Query("SELECT COUNT(f) > 0 FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.id = :id AND (pr.id IN :projetIds OR pr.id IS NULL) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    boolean existsDansPerimetre(@Param("id") UUID id, @Param("projetIds") List<UUID> projetIds);

    /**
     * Variante de {@link #existsDansPerimetre} pour un appelant <b>non restreint</b> par un
     * périmètre : seule la visibilité par entité juridique s'applique
     * ({@code instructions/FEATURE-visibilite-transversale-entites.md}, R4).
     *
     * <p>Sans elle, le filtre d'entité serait purement décoratif sur les listes : un dossier absent
     * de {@code GET /api/dossiers} resterait servi par {@code GET /api/dossiers/{id}}, échéancier,
     * pièces jointes et générateurs de documents compris. Comme pour le périmètre, un dossier
     * masqué est traité en <b>404</b>, jamais 403.
     */
    @Query("SELECT COUNT(f) > 0 FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.id = :id AND (soc.isActive = true OR soc.id IS NULL)")
    boolean existsVisible(@Param("id") UUID id);

    /** {@code GET /api/prospects/{id}/dossiers} (GAP-06). */
    Page<ComFolder> findByCfgCustomerId(UUID customerId, Pageable pageable);

    /**
     * Dossier count per client — used by {@code GET /api/clients} (server-side aggregate).
     * Tuple: [cfgCustomerId, nbDossiers].
     */
    @Query("SELECT f.cfgCustomer.id, COUNT(f) FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.cfgCustomer.id IN :clientIds AND (soc.isActive = true OR soc.id IS NULL) "
            + "GROUP BY f.cfgCustomer.id")
    List<Object[]> countGroupedByClientId(List<UUID> clientIds);

    /**
     * Batch lookup for the Article → Dossier reverse link ({@code StkItem.ComFolderId}), demande
     * #5 de {@code instructions/from_front.md} — used by {@code ArticleService} to avoid an N+1
     * query per article on a paginated listing. Join-fetches {@code strStatus}/{@code cfgCustomer}
     * so callers can read {@code getCode()}/{@code getStrStatus()}/{@code getCfgCustomer()}
     * without a further lazy-load.
     */
    @Query("SELECT f FROM ComFolder f LEFT JOIN FETCH f.strStatus LEFT JOIN FETCH f.cfgCustomer WHERE f.id IN :ids")
    List<ComFolder> findWithStatusAndCustomerByIdIn(List<UUID> ids);

    /**
     * Projection par dossier pour les KPI commerciaux — colonnes : {@code commercialId},
     * {@code totalToPay}. Une ligne par dossier plutôt qu'un {@code SUM} groupé : le service en
     * tire à la fois le compte, le montant total et le panier moyen, et les volumes sont faibles
     * (358 dossiers en base, 36 depuis 2025).
     *
     * <p>La date retenue est {@code DocDate}, renseignée sur les 358 dossiers réels.
     * {@code ReservationDate} ne l'est que sur 2 et ne peut fonder aucun indicateur.
     */
    @Query("SELECT f.cfgCommercial.id, f.totalToPay FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.docDate BETWEEN :debut AND :fin AND f.cfgCommercial.id IS NOT NULL "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    List<Object[]> projeterPourKpi(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /** Variante de {@link #projeterPourKpi} restreinte au périmètre commercial (§6.4 incluse). */
    @Query("SELECT f.cfgCommercial.id, f.totalToPay FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.docDate BETWEEN :debut AND :fin AND f.cfgCommercial.id IS NOT NULL "
            + "AND (soc.isActive = true OR soc.id IS NULL) "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL)")
    List<Object[]> projeterPourKpiDansPerimetre(@Param("debut") LocalDateTime debut,
                                                @Param("fin") LocalDateTime fin,
                                                @Param("projetIds") List<UUID> projetIds);

    /**
     * Bloc B de {@code GET /api/dashboard/annuel} — les ventes concrétisées de l'année.
     * Tuple : [nombre de dossiers, somme des prix].
     *
     * <p>Assiette du légataire, reprise telle quelle : la <b>date d'avance</b>
     * ({@code ComFolder.AdvanceDate}) tombe dans l'année, le statut a atteint le seuil de
     * concrétisation, et les sorties sont écartées. Un dossier sans date d'avance n'y entre
     * jamais — c'est le cas de 232 des 387 dossiers réels, comptés à part par
     * {@link #compterDossiersSansDateAvance()}.
     *
     * <p>La somme est rendue éventuellement {@code null} : pas de {@code COALESCE} en SQL, où le
     * littéral entier dégrade silencieusement l'échelle d'une somme d'argent. La substitution se
     * fait en Java, en {@code BigDecimal}.
     */
    @Query("SELECT COUNT(f), SUM(f.price) FROM ComFolder f JOIN f.strStatus s "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.advanceDate >= :debut AND f.advanceDate < :fin "
            + "AND s.sorting >= :seuil AND s.code NOT IN :sorties "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    List<Object[]> ventesConcretisees(LocalDateTime debut, LocalDateTime fin, int seuil,
                                      List<String> sorties);

    /** Variante de {@link #ventesConcretisees} restreinte au périmètre — même tuple. */
    @Query("SELECT COUNT(f), SUM(f.price) FROM ComFolder f JOIN f.strStatus s "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.advanceDate >= :debut AND f.advanceDate < :fin "
            + "AND s.sorting >= :seuil AND s.code NOT IN :sorties "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL) AND (soc.isActive = true OR soc.id IS NULL)")
    List<Object[]> ventesConcretiseesDansPerimetre(LocalDateTime debut, LocalDateTime fin, int seuil,
                                                   List<String> sorties, List<UUID> projetIds);

    /**
     * Les dossiers écartés du bloc B faute de date d'avance, <b>toutes années confondues</b> —
     * une date absente n'appartient à aucune année, la borner n'aurait pas de sens.
     *
     * <p>232 sur 387 en base réelle. Ce compteur existe pour que le trou d'assiette soit
     * affichable plutôt que subi : sans lui, « 4 ventes en 2026 » se lit comme un effondrement.
     */
    @Query("SELECT COUNT(f) FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.advanceDate IS NULL AND (soc.isActive = true OR soc.id IS NULL)")
    long compterDossiersSansDateAvance();

    /** Variante de {@link #compterDossiersSansDateAvance()} restreinte au périmètre. */
    @Query("SELECT COUNT(f) FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.advanceDate IS NULL "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL) AND (soc.isActive = true OR soc.id IS NULL)")
    long compterDossiersSansDateAvanceDansPerimetre(List<UUID> projetIds);
}
