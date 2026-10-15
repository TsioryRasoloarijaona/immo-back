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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ComFolderRepository extends JpaRepository<ComFolder, UUID>, JpaSpecificationExecutor<ComFolder> {

    /**
     * Charge d'un coup les associations que {@code DossierService.toListItem} lit sur chaque ligne : sans
     * ce graphe, la liste est un N+1. Uniquement des {@code ToOne} : une collection ici ferait paginer en mémoire.
     */
    @Override
    @EntityGraph(attributePaths = {"strStatus", "cfgCustomer", "cfgCommercial", "comProspection"})
    Page<ComFolder> findAll(Specification<ComFolder> spec, Pageable pageable);

    /**
     * Tuple : [id, statutCode, totalToPay, revival1, revival1ValidityDate, revival2,
     * revival2ValidityDate, customerSocialReason, customerFirstName, customerLastName].
     */
    @Query("SELECT f.id, s.code, f.totalToPay, f.revival1, f.revival1ValidityDate, f.revival2, f.revival2ValidityDate, " +
            "c.socialReason, c.firstName, c.lastName " +
            "FROM ComFolder f LEFT JOIN f.strStatus s LEFT JOIN f.cfgCustomer c " +
            "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc " +
            "WHERE (soc.isActive = true OR soc.id IS NULL)")
    List<Object[]> findDashboardProjection();

    /**
     * Même tuple, dans le périmètre. Projet par la tranche de l'article ({@code StkItem.CfgProjectId} est
     * mort) ; {@code LEFT JOIN} + {@code pr.id IS NULL} : un dossier sans projet reste visible de tous
     * (FEATURE-perimetre-commercial.md §6.4), une jointure interne le masquerait.
     */
    @Query("SELECT f.id, s.code, f.totalToPay, f.revival1, f.revival1ValidityDate, f.revival2, f.revival2ValidityDate, " +
            "c.socialReason, c.firstName, c.lastName " +
            "FROM ComFolder f LEFT JOIN f.strStatus s LEFT JOIN f.cfgCustomer c " +
            "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc " +
            "WHERE (pr.id IN :projetIds OR pr.id IS NULL) AND (soc.isActive = true OR soc.id IS NULL)")
    List<Object[]> findDashboardProjectionDansPerimetre(@Param("projetIds") List<UUID> projetIds);

    /**
     * Contrôle unique du périmètre par id ({@code PerimetreInterceptor}) ; vrai aussi sans projet résolu
     * (§6.4). Hors périmètre = 404, jamais 403 : sinon les ids deviennent énumérables.
     */
    @Query("SELECT COUNT(f) > 0 FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.id = :id AND (pr.id IN :projetIds OR pr.id IS NULL) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    boolean existsDansPerimetre(@Param("id") UUID id, @Param("projetIds") List<UUID> projetIds);

    /**
     * Appelant sans périmètre : seule la visibilité par entité s'applique (FEATURE-visibilite-transversale-entites.md
     * R4), sinon un dossier absent des listes resterait servi par id. Masqué = 404, jamais 403.
     */
    @Query("SELECT COUNT(f) > 0 FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.id = :id AND (soc.isActive = true OR soc.id IS NULL)")
    boolean existsVisible(@Param("id") UUID id);

    Page<ComFolder> findByCfgCustomerId(UUID customerId, Pageable pageable);

    /**
     * Le tiers, client principal ou acquéreur ({@code ComFolderCustomer}), a un dossier dont le statut
     * transforme en client ({@code BoolField011}) et n'est ni annulé ni résilié — {@code Sale.cs:ManageTier}.
     */
    @Query("SELECT COUNT(f) > 0 FROM ComFolder f JOIN f.strStatus s "
            + "WHERE (f.cfgCustomer.id = :tierId OR EXISTS (SELECT fc.id FROM ComFolderCustomer fc "
            + "WHERE fc.comFolder = f AND fc.cfgTier.id = :tierId)) "
            + "AND s.transformeProspectEnClient = true "
            + "AND (s.annulation IS NULL OR s.annulation = false) "
            + "AND (s.resiliation IS NULL OR s.resiliation = false)")
    boolean aUnDossierActifClient(@Param("tierId") UUID tierId);

    /** Même règle pour un dossier de location ({@code RntFolder}, sans entité) : statut {@code BoolField001} actif. */
    @Query(value = "SELECT COUNT(*) FROM RntFolder r INNER JOIN StrStatus s ON s.PKey = r.StrStatusId "
            + "WHERE r.CfgCustomerId = :tierId AND s.BoolField001 = 1 "
            + "AND ISNULL(s.BoolField002, 0) = 0 AND ISNULL(s.BoolField003, 0) = 0", nativeQuery = true)
    long compterLocationsActives(@Param("tierId") UUID tierId);

    /** [id, code] des dossiers de vente créés depuis l'offre ({@code ComFolder.StlQuoteId}). */
    @Query("SELECT f.id, f.code FROM ComFolder f WHERE f.stlQuoteId = :offreId ORDER BY f.code ASC")
    List<Object[]> dossiersDeLOffre(@Param("offreId") UUID offreId);

    /** {@code Prospection.cs:VerifComFolderIsCreated} : tout dossier de vente, sorti compris. */
    boolean existsByComProspectionId(UUID prospectionId);

    /** Codes des dossiers de location créés depuis l'offre ({@code RntFolder.StlQuoteId}, FK sans cascade). */
    @Query(value = "SELECT ISNULL(r.Code, '') FROM RntFolder r WHERE r.StlQuoteId = :offreId ORDER BY r.Code ASC",
            nativeQuery = true)
    List<String> locationsDeLOffre(@Param("offreId") UUID offreId);

    /** Dossiers de syndic rattachés et en cours (statut ni {@code BoolField001} ni {@code BoolField002}), {@code Sale.cs:BeforeUpdate}. */
    @Query(value = "SELECT COUNT(*) FROM SynFolder sf INNER JOIN StrStatus s ON sf.StrStatusId = s.PKey "
            + "WHERE s.BoolField001 = 0 AND s.BoolField002 = 0 AND sf.ComFolderId = :dossierId", nativeQuery = true)
    long compterSyndicsEnCours(@Param("dossierId") UUID dossierId);

    /** Factures du dossier nettes de leurs avoirs (HT, {@code REFUND} en négatif), {@code Sale.cs:Verif}. */
    @Query(value = "SELECT ISNULL(SUM(l.TotalHT * IIF(d.DocType = 'REFUND', -1, 1)), 0) FROM StlDocumentLine l "
            + "INNER JOIN StlDocument d ON l.StlDocumentId = d.PKey WHERE l.ComFolderId = :dossierId",
            nativeQuery = true)
    BigDecimal soldeFacturesNetDesAvoirs(@Param("dossierId") UUID dossierId);

    /** Tuple : [cfgCustomerId, nbDossiers]. */
    @Query("SELECT f.cfgCustomer.id, COUNT(f) FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.cfgCustomer.id IN :clientIds AND (soc.isActive = true OR soc.id IS NULL) "
            + "GROUP BY f.cfgCustomer.id")
    List<Object[]> countGroupedByClientId(List<UUID> clientIds);

    /** Lien article → dossier par lot, statut et client chargés : évite un N+1 par article. */
    @Query("SELECT f FROM ComFolder f LEFT JOIN FETCH f.strStatus LEFT JOIN FETCH f.cfgCustomer WHERE f.id IN :ids")
    List<ComFolder> findWithStatusAndCustomerByIdIn(List<UUID> ids);

    /**
     * [commercialId, totalToPay], une ligne par dossier : le service en tire compte, total et panier moyen.
     * Date = {@code DocDate} : {@code ReservationDate} n'est presque jamais renseignée.
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
     * Bloc B de {@code /api/dashboard/annuel}, assiette du légataire : [nombre, somme des prix] par
     * {@code AdvanceDate} (sans elle, jamais compté). Somme {@code null} possible : un {@code COALESCE}
     * à littéral entier arrondirait l'argent, le zéro se substitue en Java.
     */
    @Query("SELECT COUNT(f), SUM(f.price) FROM ComFolder f JOIN f.strStatus s "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.advanceDate >= :debut AND f.advanceDate < :fin "
            + "AND s.sorting >= :seuil AND s.code NOT IN :sorties "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    List<Object[]> ventesConcretisees(LocalDateTime debut, LocalDateTime fin, int seuil,
                                      List<String> sorties);

    /** Filtre par tranche : {@code dossierIds} = assiette de {@code /api/dossiers?trancheId=}, jamais vide. */
    @Query("SELECT COUNT(f), SUM(f.price) FROM ComFolder f JOIN f.strStatus s "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.advanceDate >= :debut AND f.advanceDate < :fin "
            + "AND s.sorting >= :seuil AND s.code NOT IN :sorties "
            + "AND (soc.isActive = true OR soc.id IS NULL) "
            + "AND f.id IN :dossierIds")
    List<Object[]> ventesConcretiseesPourDossiers(LocalDateTime debut, LocalDateTime fin, int seuil,
                                      List<String> sorties, List<UUID> dossierIds);

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
     * Dossiers écartés du bloc B faute de date d'avance, toutes années confondues : rend le trou
     * d'assiette affichable (FEATURE-dashboard-omran.md).
     */
    @Query("SELECT COUNT(f) FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.advanceDate IS NULL AND (soc.isActive = true OR soc.id IS NULL)")
    long compterDossiersSansDateAvance();

    /** Filtre par tranche : {@code dossierIds} = assiette de {@code /api/dossiers?trancheId=}, jamais vide. */
    @Query("SELECT COUNT(f) FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.advanceDate IS NULL AND (soc.isActive = true OR soc.id IS NULL) "
            + "AND f.id IN :dossierIds")
    long compterDossiersSansDateAvancePourDossiers(List<UUID> dossierIds);

    /** Variante de {@link #compterDossiersSansDateAvance()} restreinte au périmètre. */
    @Query("SELECT COUNT(f) FROM ComFolder f "
            + "LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE f.advanceDate IS NULL "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL) AND (soc.isActive = true OR soc.id IS NULL)")
    long compterDossiersSansDateAvanceDansPerimetre(List<UUID> projetIds);
}
