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

    /** Article lu ou écrit par id : entité de sa tranche inactive → absent (R4) ; sans tranche ni entité = visible. */
    @Query("SELECT i FROM StkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc "
            + "WHERE i.id = :id AND " + VisibiliteEntites.CLAUSE_JPQL)
    Optional<StkItem> findVisible(@Param("id") UUID id);

    // StkHierarchyId = la tranche. Les listes filtrées passent par StkItemSpecifications.
    List<StkItem> findByStkHierarchyId(UUID trancheId);

    /** Articles principaux de la tranche d'une nature ({@code StkKind.Code}) : cible de l'attachement en masse. */
    @Query("SELECT i FROM StkItem i WHERE i.stkHierarchy.id = :trancheId AND i.stkKind.code = :nature "
            + "AND (i.isUnderItem IS NULL OR i.isUnderItem = false)")
    List<StkItem> findPrincipauxDeNature(@Param("trancheId") UUID trancheId, @Param("nature") String nature);

    // Sans filtre de statut : la fiche article montre tous ses sous-articles.
    List<StkItem> findByParentId(UUID parentId);

    // Un dossier peut lier plusieurs articles (principal + annexe « TER »).
    List<StkItem> findByComFolderId(UUID comFolderId);

    /** « Superficie Sous Articles » par lot de parents. Tuple : [parentId, sommeSuperficie]. */
    @Query("SELECT i.parent.id, COALESCE(SUM(i.area), 0) FROM StkItem i " +
            "WHERE i.parent.id IN :parentIds GROUP BY i.parent.id")
    List<Object[]> sumAreaGroupedByParentIdIn(List<UUID> parentIds);

    /** Unicité du code par tranche, sous-articles compris (collation French_CI_AS : sans casse). */
    Optional<StkItem> findFirstByStkHierarchyIdAndCode(UUID trancheId, String code);

    /** Filtre OMRAN {@code BuildItem:AffectUnderItem} + nos deux garde-fous d'occupation (FEATURE-patrimoine-ecriture.md §5.1). */
    @Query("SELECT i FROM StkItem i WHERE i.stkHierarchy.id = :trancheId AND i.isUnderItem = true "
            + "AND i.alwaysAffected = false AND i.parent IS NULL "
            + "AND i.comFolderId IS NULL AND i.rntFolderId IS NULL")
    Page<StkItem> findSousArticlesLibres(@Param("trancheId") UUID trancheId, Pageable pageable);
}
