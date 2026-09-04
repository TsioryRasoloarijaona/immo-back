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
            "FROM ComFolder f LEFT JOIN f.strStatus s LEFT JOIN f.cfgCustomer c")
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
            "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr " +
            "WHERE pr.id IN :projetIds OR pr.id IS NULL")
    List<Object[]> findDashboardProjectionDansPerimetre(@Param("projetIds") List<UUID> projetIds);

    /**
     * Point de contrôle unique du périmètre sur le chargement d'un dossier par id (voir
     * {@code PerimetreInterceptor}) : le dossier existe-t-il ET est-il dans le périmètre ?
     * Renvoie aussi {@code true} pour un dossier dont le projet ne se résout pas (§6.4).
     * Un dossier hors périmètre est traité exactement comme un dossier inexistant — 404, jamais
     * 403, sinon l'existence de la ressource serait confirmée et les ids énumérables.
     */
    @Query("SELECT COUNT(f) > 0 FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "WHERE f.id = :id AND (pr.id IN :projetIds OR pr.id IS NULL)")
    boolean existsDansPerimetre(@Param("id") UUID id, @Param("projetIds") List<UUID> projetIds);

    /** {@code GET /api/prospects/{id}/dossiers} (GAP-06). */
    Page<ComFolder> findByCfgCustomerId(UUID customerId, Pageable pageable);

    /**
     * Dossier count per client — used by {@code GET /api/clients} (server-side aggregate).
     * Tuple: [cfgCustomerId, nbDossiers].
     */
    @Query("SELECT f.cfgCustomer.id, COUNT(f) FROM ComFolder f WHERE f.cfgCustomer.id IN :clientIds GROUP BY f.cfgCustomer.id")
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
}
