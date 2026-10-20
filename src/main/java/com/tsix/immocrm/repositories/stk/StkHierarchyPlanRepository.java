package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.dto.patrimoine.plans.PlanArticleDto;
import com.tsix.immocrm.dto.patrimoine.plans.PlanDto;
import com.tsix.immocrm.entities.stk.StkHierarchyPlan;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@code PlanImage} n'est lue ou écrite qu'ici, en SQL natif (colonne absente de l'entité). */
public interface StkHierarchyPlanRepository extends JpaRepository<StkHierarchyPlan, UUID> {

    String DTO = "com.tsix.immocrm.dto.patrimoine.plans.";
    String DEPUIS_PLAN_ETAGE = " FROM StkHierarchyPlan p LEFT JOIN StkFloor f ON f.id = p.stkFloorId WHERE p.stkHierarchyId = :trancheId ";

    // Un étage est partagé entre tranches : un tracé du plan = même étage ET article de la tranche du plan.
    @Query("SELECT new " + DTO + "PlanDto(p.id, f.id, f.code, p.code, p.largeur, p.hauteur, p.x, p.y, "
            + "(SELECT COUNT(h) FROM StkHierarchyPath h JOIN StkItem i ON i.id = h.stkItemId "
            + "WHERE h.stkFloorId = p.stkFloorId AND i.stkHierarchy.id = p.stkHierarchyId))"
            + DEPUIS_PLAN_ETAGE + "ORDER BY f.sorting, p.code")
    List<PlanDto> plansDeLaTranche(UUID trancheId);

    @Query("SELECT new " + DTO + "PlanArticleDto(p.id, p.stkHierarchyId, f.id, f.code, p.largeur, p.hauteur, p.x, p.y)"
            + DEPUIS_PLAN_ETAGE
            + "AND p.stkFloorId IN (SELECT h.stkFloorId FROM StkHierarchyPath h WHERE h.stkItemId = :articleId) "
            + "ORDER BY f.sorting, p.code")
    List<PlanArticleDto> plansDeLArticle(UUID trancheId, UUID articleId);

    /** Tranches visibles (entité active) ayant un plan : id, code, libellé, projet (id, code), nombre de plans. */
    @Query("SELECT t.id, t.code, t.description, pr.id, pr.code, COUNT(p) FROM StkHierarchyPlan p, CfgTranche t "
            + "LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc WHERE t.id = p.stkHierarchyId AND "
            + VisibiliteEntites.CLAUSE_JPQL + " GROUP BY t.id, t.code, t.description, pr.id, pr.code ORDER BY t.code")
    List<Object[]> tranchesVisiblesAvecPlans();

    List<StkHierarchyPlan> findByStkHierarchyId(UUID trancheId);

    Optional<StkHierarchyPlan> findFirstByStkHierarchyIdAndStkFloorIdOrderByCode(UUID trancheId, UUID etageId);

    /** ETag sans lire l'image : empreinte de la clé, de la taille de l'image et des dates de création / mise à jour. */
    @Query(value = "SELECT CONVERT(varchar(64), HASHBYTES('SHA2_256', CONCAT(CONVERT(varchar(36), PKey), '|', "
            + "DATALENGTH(PlanImage), '|', CONVERT(varchar(23), UserCreateDateTime, 126), '|', "
            + "CONVERT(varchar(23), UserUpdateDateTime, 126))), 2) FROM StkHierarchyPlan WHERE PKey = :planId",
            nativeQuery = true)
    Optional<String> empreinteImage(UUID planId);

    @Query(value = "SELECT PlanImage FROM StkHierarchyPlan WHERE PKey = :planId", nativeQuery = true)
    Optional<String> image(UUID planId);

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE StkHierarchyPlan SET PlanImage = :image WHERE PKey = :planId", nativeQuery = true)
    void poserImage(UUID planId, String image);
}
