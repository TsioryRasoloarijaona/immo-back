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

    /** Tranches dont l'entité juridique du projet parent est en exploitation — voir {@code CfgCompany.isActive}. */
    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.cfgCompany.isActive = true")
    List<CfgTranche> findDesEntitesActives();

    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.cfgCompany.isActive = true")
    Page<CfgTranche> findDesEntitesActives(Pageable pageable);

    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.id = :projetId AND t.cfgProject.cfgCompany.isActive = true")
    List<CfgTranche> findDesEntitesActivesParProjet(@Param("projetId") UUID projetId);

    @Query("SELECT t FROM CfgTranche t WHERE t.cfgProject.id = :projetId AND t.cfgProject.cfgCompany.isActive = true")
    Page<CfgTranche> findDesEntitesActivesParProjet(@Param("projetId") UUID projetId, Pageable pageable);

    /**
     * Tranches auxquelles un prospect s'est intéressé, toutes prospections confondues.
     *
     * <p>Reproduit la règle de {@code ComSaleProspectView.CfgTranches} / {@code CfgTrancheIds},
     * lue dans {@code sys.sql_modules} : union <strong>distincte</strong> de la tranche
     * <em>principale</em> de chaque prospection du prospect et de ses <em>autres tranches</em>
     * ({@code ComProspectionProject}, les intérêts secondaires). Un prospect qui a la même tranche
     * en principale et en secondaire ne la voit donc qu'une fois.
     *
     * <p>Deux sous-requêtes {@code IN} plutôt qu'un {@code UNION} : JPQL ne porte pas les
     * opérations ensemblistes de façon portable, et le résultat est de toute façon minuscule —
     * mesuré sur les 13 664 prospects réels, <strong>13 246 ont une seule tranche, 254 en ont deux
     * et 8 en ont trois</strong>. Aucune raison de paginer ni de conditionner cet appel.
     *
     * @return liste triée par libellé, vide si le prospect n'a aucune prospection.
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
}
