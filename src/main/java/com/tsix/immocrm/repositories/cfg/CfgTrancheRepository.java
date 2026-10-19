package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfgTrancheRepository extends JpaRepository<CfgTranche, UUID> {

    List<CfgTranche> findByCfgProjectId(UUID projectId);

    Page<CfgTranche> findByCfgProjectId(UUID projectId, Pageable pageable);

    /** Entité du projet en exploitation ; chemin implicite = jointure interne, sans projet ni société = exclue. */
    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.cfgCompany.isActive = true")
    List<CfgTranche> findDesEntitesActives();

    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.id = :projetId AND t.cfgProject.cfgCompany.isActive = true")
    List<CfgTranche> findDesEntitesActivesParProjet(@Param("projetId") UUID projetId);

    /** {@code motif} = {@code %texte%} en minuscules (code, libellé, ville, programme), ou null pour tout. */
    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.cfgCompany.isActive = true "
            + "AND (:projetId IS NULL OR t.cfgProject.id = :projetId) AND (:motif IS NULL "
            + "OR lower(t.code) LIKE :motif OR lower(t.description) LIKE :motif OR lower(t.city) LIKE :motif "
            + "OR lower(t.cfgProject.code) LIKE :motif)")
    Page<CfgTranche> rechercher(@Param("projetId") UUID projetId, @Param("motif") String motif, Pageable pageable);

    /**
     * Union distincte des tranches principales et secondaires ({@code ComProspectionProject}) du prospect,
     * comme {@code ComSaleProspectView.CfgTranches}. Deux {@code IN} : JPQL ne porte pas {@code UNION} de
     * façon portable ; résultat minuscule (FEATURE-audit-tranches-prospect.md).
     */
    @Query("""
            SELECT DISTINCT t FROM CfgTranche t
            WHERE t.id IN (SELECT p.stkHierarchy.id FROM ComProspection p
                           WHERE p.cfgProspect.id = :prospectId)
               OR t.id IN (SELECT pp.stkHierarchy.id FROM ComProspectionProject pp
                           WHERE pp.comProspection.cfgProspect.id = :prospectId)
            ORDER BY t.description
            """)
    List<CfgTranche> tranchesInteret(@Param("prospectId") UUID prospectId);

    /** Tranche d'une entité en exploitation (lecture par id d'une sous-ressource). */
    boolean existsByIdAndCfgProject_CfgCompany_IsActiveTrue(UUID id);

    /** R4 : entité active ou absente. */
    @Query(TrancheVisibleRepository.TRANCHE_VISIBLE)
    Optional<CfgTranche> trancheVisible(@Param("id") UUID id);

    /** Par code exact — sert la tranche suggérée à l'import CSV des leads. */
    Optional<CfgTranche> findFirstByCode(String code);
}
