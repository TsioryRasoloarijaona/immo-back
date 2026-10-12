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

    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.cfgCompany.isActive = true")
    Page<CfgTranche> findDesEntitesActives(Pageable pageable);

    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.id = :projetId AND t.cfgProject.cfgCompany.isActive = true")
    List<CfgTranche> findDesEntitesActivesParProjet(@Param("projetId") UUID projetId);

    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.id = :projetId AND t.cfgProject.cfgCompany.isActive = true")
    Page<CfgTranche> findDesEntitesActivesParProjet(@Param("projetId") UUID projetId, Pageable pageable);

    /**
     * Liste {@code GET /api/tranches} : programme facultatif ; {@code motif} = {@code %texte%} en minuscules ou
     * {@code null}, sur code, libellé, ville, programme et entité.
     */
    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.cfgCompany.isActive = true "
            + "AND (:projetId IS NULL OR t.cfgProject.id = :projetId) AND (:motif IS NULL "
            + "OR lower(t.code) LIKE :motif OR lower(t.description) LIKE :motif OR lower(t.city) LIKE :motif "
            + "OR lower(t.cfgProject.code) LIKE :motif OR lower(t.cfgProject.description) LIKE :motif "
            + "OR lower(t.cfgProject.cfgCompany.description) LIKE :motif)")
    Page<CfgTranche> rechercherDesEntitesActives(@Param("projetId") UUID projetId, @Param("motif") String motif,
                                                 Pageable pageable);

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

    /** Par code exact — sert la tranche suggérée à l'import CSV des leads. */
    Optional<CfgTranche> findFirstByCode(String code);

    /** Unicité du code sur toute la table, pas par programme (collation French_CI_AS). */
    Optional<CfgTranche> findFirstByCodeAndIdNot(String code, UUID id);
}
