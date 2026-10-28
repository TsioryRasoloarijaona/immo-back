package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StkItemRepository extends JpaRepository<StkItem, UUID>, JpaSpecificationExecutor<StkItem> {

    /**
     * Sans tranche (terrain), la société directe {@code CfgCompanyId} décide ; avec tranche, celle du programme seule :
     * des articles bâtis portent une société directe inactive sous une tranche active (FEATURE-patrimoine-terrains.md).
     */
    String SOCIETE_DIRECTE = "(t.id IS NOT NULL OR socd.isActive = true OR socd.id IS NULL)";

    /** Article lu ou écrit par id : entité de sa tranche, sinon sa société directe, inactive → absent (R4). */
    @Query("SELECT i FROM StkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc "
            + "LEFT JOIN i.cfgCompany socd WHERE i.id = :id AND " + VisibiliteEntites.CLAUSE_JPQL + " AND "
            + SOCIETE_DIRECTE)
    Optional<StkItem> findVisible(@Param("id") UUID id);

    // StkHierarchyId = la tranche. Les listes filtrées passent par StkItemSpecifications.
    List<StkItem> findByStkHierarchyId(UUID trancheId);

    /** Articles principaux de la tranche d'une nature ({@code StkKind.Code}) : cible de l'attachement en masse. */
    @Query("SELECT i FROM StkItem i WHERE i.stkHierarchy.id = :trancheId AND i.stkKind.code = :nature "
            + "AND (i.isUnderItem IS NULL OR i.isUnderItem = false)")
    List<StkItem> findPrincipauxDeNature(@Param("trancheId") UUID trancheId, @Param("nature") String nature);

    // Sans filtre de statut : la fiche article montre tous ses sous-articles.
    List<StkItem> findByParentId(UUID parentId);

    /** Terrain issu d'une prospection terrain ({@code LndLandId}). */
    Optional<StkItem> findFirstByLndLandId(UUID prospectionId);

    // Un dossier peut lier plusieurs articles (principal + annexe « TER »).
    List<StkItem> findByComFolderId(UUID comFolderId);

    /** « Superficie Sous Articles » par lot de parents. Tuple : [parentId, sommeSuperficie]. */
    @Query("SELECT i.parent.id, COALESCE(SUM(i.area), 0) FROM StkItem i " +
            "WHERE i.parent.id IN :parentIds GROUP BY i.parent.id")
    List<Object[]> sumAreaGroupedByParentIdIn(List<UUID> parentIds);

    /** « Prix Sous Article » par lot de parents. Tuple : [parentId, ΣSalePriceHT, ΣSalePrice, ΣSalePriceFloorHT, ΣSalePriceFloor]. */
    @Query("SELECT i.parent.id, COALESCE(SUM(i.salePriceHT), 0), COALESCE(SUM(i.salePrice), 0), "
            + "COALESCE(SUM(i.salePriceFloorHT), 0), COALESCE(SUM(i.salePriceFloor), 0) FROM StkItem i "
            + "WHERE i.parent.id IN :parentIds GROUP BY i.parent.id")
    List<Object[]> sumPrixGroupedByParentIdIn(List<UUID> parentIds);

    /** « Sous articles » par lot de parents. Tuple : [parentId, code]. */
    @Query("SELECT i.parent.id, i.code FROM StkItem i WHERE i.parent.id IN :parentIds ORDER BY i.code ASC")
    List<Object[]> codesByParentIdIn(List<UUID> parentIds);

    /** Superficies titrée et d'une catégorie des sous-articles par lot de parents. Tuple : [parentId, Σtitrés, Σcatégorie]. */
    @Query("SELECT i.parent.id, COALESCE(SUM(CASE WHEN i.isTitled = true THEN i.area END), 0), "
            + "COALESCE(SUM(CASE WHEN c.code = :categorie THEN i.area END), 0) FROM StkItem i "
            + "LEFT JOIN i.stkItemCategory c WHERE i.parent.id IN :parentIds GROUP BY i.parent.id")
    List<Object[]> sumAreaTitreeEtCategorieGroupedByParentIdIn(List<UUID> parentIds, String categorie);

    /** Codes des sous-articles d'une catégorie par lot de parents. Tuple : [parentId, code]. */
    @Query("SELECT i.parent.id, i.code FROM StkItem i WHERE i.parent.id IN :parentIds "
            + "AND i.stkItemCategory.code = :categorie")
    List<Object[]> codesByParentIdInAndCategorie(List<UUID> parentIds, String categorie);

    /** Libellés des vues par lot d'articles. Tuple : [articleId, libellé]. */
    @Query("SELECT v.stkItem.id, v.stkVue.description FROM StkItemVue v WHERE v.stkItem.id IN :ids "
            + "ORDER BY v.stkVue.description ASC")
    List<Object[]> vuesByStkItemIdIn(List<UUID> ids);

    /** Unicité Code + société d'un terrain ({@code nature} = TER). */
    boolean existsByCfgCompany_IdAndCodeAndStkKind_CodeAndIdNot(UUID societeId, String code, String nature, UUID id);

    /** Unicité du code par tranche, sous-articles compris (collation French_CI_AS : sans casse). */
    Optional<StkItem> findFirstByStkHierarchyIdAndCode(UUID trancheId, String code);

    /** Filtre OMRAN {@code BuildItem:AffectUnderItem} + nos deux garde-fous d'occupation (FEATURE-patrimoine-ecriture.md §5.1). */
    @Query("SELECT i FROM StkItem i WHERE i.stkHierarchy.id = :trancheId AND i.isUnderItem = true "
            + "AND i.alwaysAffected = false AND i.parent IS NULL "
            + "AND i.comFolderId IS NULL AND i.rntFolderId IS NULL")
    Page<StkItem> findSousArticlesLibres(@Param("trancheId") UUID trancheId, Pageable pageable);
}
