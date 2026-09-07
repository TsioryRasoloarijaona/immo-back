package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ComProspectionRepository extends JpaRepository<ComProspection, UUID>, JpaSpecificationExecutor<ComProspection> {

    // Pré-prospection: CfgProspectId IS NULL AND CfgCommercialId IS NULL (confirmed).
    List<ComProspection> findByCfgProspectIsNullAndCfgCommercialIsNull();

    Page<ComProspection> findByCfgProspectIsNullAndCfgCommercialIsNull(Pageable pageable);

    /**
     * Real prospections (excludes pré-prospections), for {@code GET /api/dashboard} — restreint aux
     * entités juridiques en exploitation, les prospections sans tranche restant comptées (R2).
     */
    @Query("SELECT COUNT(p) FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE NOT (p.cfgProspect IS NULL AND p.cfgCommercial IS NULL) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    long countExcludingPreProspections();

    /** Real, non-closed prospections, for {@code GET /api/dashboard} — même restriction d'entité. */
    @Query("SELECT COUNT(p) FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE NOT (p.cfgProspect IS NULL AND p.cfgCommercial IS NULL) "
            + "AND (p.isClosed IS NULL OR p.isClosed = false) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    long countOpenExcludingPreProspections();

    /**
     * Variante de {@link #countExcludingPreProspections()} restreinte au périmètre commercial.
     * Le projet d'une prospection se résout <b>directement</b> par sa tranche
     * ({@code ComProspection.StkHierarchyId} → {@code CfgTranche.CfgProjectId}), sans passer par
     * {@code StkItem} : cette colonne n'est renseignée que sur 4,61 % des lignes et n'apporte
     * aucune ligne supplémentaire (mesuré). {@code pr.id IS NULL} reste inclus — une prospection
     * sans projet résoluble n'est masquée à personne (§6.4).
     */
    @Query("SELECT COUNT(p) FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE NOT (p.cfgProspect IS NULL AND p.cfgCommercial IS NULL) "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    long countExcludingPreProspectionsDansPerimetre(@Param("projetIds") List<UUID> projetIds);

    /** Variante de {@link #countOpenExcludingPreProspections()} restreinte au périmètre commercial. */
    @Query("SELECT COUNT(p) FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE NOT (p.cfgProspect IS NULL AND p.cfgCommercial IS NULL) "
            + "AND (p.isClosed IS NULL OR p.isClosed = false) "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    long countOpenExcludingPreProspectionsDansPerimetre(@Param("projetIds") List<UUID> projetIds);

    /**
     * Point de contrôle unique du périmètre sur le chargement d'une prospection (ou
     * pré-prospection) par id — voir {@code PerimetreInterceptor}. Hors périmètre ⇒ traitée comme
     * inexistante (404), jamais 403.
     */
    @Query("SELECT COUNT(p) > 0 FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE p.id = :id AND (pr.id IN :projetIds OR pr.id IS NULL) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    boolean existsDansPerimetre(@Param("id") UUID id, @Param("projetIds") List<UUID> projetIds);

    /**
     * Variante de {@link #existsDansPerimetre} pour un appelant non restreint par un périmètre :
     * seule la visibilité par entité juridique s'applique (R4). Une prospection sans tranche reste
     * accessible (R2).
     */
    @Query("SELECT COUNT(p) > 0 FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE p.id = :id AND (soc.isActive = true OR soc.id IS NULL)")
    boolean existsVisible(@Param("id") UUID id);

    /** {@code GET /api/prospects/{id}/prospections} (GAP-06). */
    Page<ComProspection> findByCfgProspectId(UUID prospectId, Pageable pageable);

    /**
     * {@code GET /api/hotline/recherche} — every {@code ComProspection} whose {@code TempTel}
     * (the free-text phone captured before a caller has a {@code CfgTier} record, e.g.
     * pré-prospections) matches the given number. Same format-agnostic normalization as
     * {@link com.tsix.immocrm.repositories.cfg.CfgTierRepository#findByNormalizedPhone}.
     */
    @Query("SELECT p FROM ComProspection p WHERE "
            + "right(replace(replace(replace(replace(replace(replace(coalesce(p.tempTel, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :normalizedPhone")
    Page<ComProspection> findByNormalizedTempTel(@Param("normalizedPhone") String normalizedPhone, Pageable pageable);

    /**
     * Variante de {@link #findByNormalizedTempTel} restreinte au périmètre commercial.
     * <p>
     * Le filtrage doit impérativement être <b>dans la requête</b> et non appliqué après coup sur la
     * page renvoyée : {@code HotlineService} plafonne cette liste à 50 lignes, et filtrer ces 50
     * lignes en Java évincerait les lignes <em>du</em> périmètre — celles hors périmètre ayant déjà
     * consommé la page — au lieu d'évincer celles hors périmètre. Voir §9ter.2.
     */
    @Query("SELECT p FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr WHERE "
            + "right(replace(replace(replace(replace(replace(replace(coalesce(p.tempTel, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :normalizedPhone "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL)")
    Page<ComProspection> findByNormalizedTempTelDansPerimetre(@Param("normalizedPhone") String normalizedPhone,
                                                               @Param("projetIds") List<UUID> projetIds,
                                                               Pageable pageable);
}
