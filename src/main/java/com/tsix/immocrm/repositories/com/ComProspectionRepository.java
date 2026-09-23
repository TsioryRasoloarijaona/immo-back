package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ComProspectionRepository extends JpaRepository<ComProspection, UUID>, JpaSpecificationExecutor<ComProspection> {

    // Pré-prospection : StrEntity.Code = 'ComPreProspection' — voir DiscriminantPreProspection.
    // Les requêtes ci-dessous en portent le complément exact, NULL compris.

    /**
     * Real prospections (excludes pré-prospections), for {@code GET /api/dashboard} — restreint aux
     * entités juridiques en exploitation, les prospections sans tranche restant comptées (R2).
     */
    @Query("SELECT COUNT(p) FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE (p.strEntityId IS NULL OR p.strEntityId NOT IN (SELECT s.id FROM StrEntity s WHERE s.code = 'ComPreProspection')) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    long countExcludingPreProspections();

    /** Real, non-closed prospections, for {@code GET /api/dashboard} — même restriction d'entité. */
    @Query("SELECT COUNT(p) FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE (p.strEntityId IS NULL OR p.strEntityId NOT IN (SELECT s.id FROM StrEntity s WHERE s.code = 'ComPreProspection')) "
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
            + "WHERE (p.strEntityId IS NULL OR p.strEntityId NOT IN (SELECT s.id FROM StrEntity s WHERE s.code = 'ComPreProspection')) "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    long countExcludingPreProspectionsDansPerimetre(@Param("projetIds") List<UUID> projetIds);

    /** Variante de {@link #countOpenExcludingPreProspections()} restreinte au périmètre commercial. */
    @Query("SELECT COUNT(p) FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE (p.strEntityId IS NULL OR p.strEntityId NOT IN (SELECT s.id FROM StrEntity s WHERE s.code = 'ComPreProspection')) "
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

    /**
     * Projection par prospection pour les KPI commerciaux — colonnes : {@code commercialId},
     * {@code docDate}, {@code isClosed}, date de la <b>première</b> interaction reçue (toutes
     * origines confondues), {@code null} si jamais contactée.
     *
     * <p>Une ligne par prospection plutôt qu'un agrégat SQL : la médiane du délai de première
     * réponse ne s'exprime pas en JPQL, et le taux de prise en charge se déduit du même passage.
     * Même parti-pris que {@code DashboardService}, qui agrège en Java une projection de dossiers.
     *
     * <p>{@code p.cfgCommercial.id IS NOT NULL} exclut mécaniquement les pré-prospections, qui
     * n'ont par construction aucun commercial (0 sur 2 800 mesurées).
     *
     * <p>La sous-requête {@code MIN} porte sur <b>toutes</b> les interactions de la prospection,
     * pas seulement celles de son commercial : ce qu'on mesure est le délai avant que le prospect
     * reçoive une réponse, pas la performance individuelle du titulaire.
     */
    @Query("SELECT p.cfgCommercial.id, p.docDate, p.isClosed, "
            + "(SELECT MIN(m.docDate) FROM ComActionMessage m WHERE m.comProspection = p) "
            + "FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE p.docDate BETWEEN :debut AND :fin AND p.cfgCommercial.id IS NOT NULL "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    List<Object[]> projeterPourKpi(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /** Variante de {@link #projeterPourKpi} restreinte au périmètre commercial (§6.4 incluse). */
    @Query("SELECT p.cfgCommercial.id, p.docDate, p.isClosed, "
            + "(SELECT MIN(m.docDate) FROM ComActionMessage m WHERE m.comProspection = p) "
            + "FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE p.docDate BETWEEN :debut AND :fin AND p.cfgCommercial.id IS NOT NULL "
            + "AND (soc.isActive = true OR soc.id IS NULL) "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL)")
    List<Object[]> projeterPourKpiDansPerimetre(@Param("debut") LocalDateTime debut,
                                                @Param("fin") LocalDateTime fin,
                                                @Param("projetIds") List<UUID> projetIds);

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

    /**
     * Pendant par NOM de {@link #findByNormalizedTempTel} : pré-prospections dont le nom saisi à
     * l'époque ({@code TempDescription}) contient le terme. Cette colonne est renseignée sur
     * 3 569 des 3 574 pré-prospections sans fiche (99,9 %) — mieux que {@code TempTel} lui-même.
     *
     * <p>Native pour la même raison que
     * {@link com.tsix.immocrm.repositories.cfg.CfgTierRepository#rechercherParNom} : la base est
     * en {@code French_CI_AS}, sensible aux accents, et JPQL ne sait pas exprimer
     * {@code COLLATE}.
     */
    @Query(value = "SELECT TOP (:limite) p.* FROM ComProspection p "
            + "WHERE p.TempDescription COLLATE French_CI_AI LIKE :motif "
            + "ORDER BY p.DocDate DESC",
            nativeQuery = true)
    List<ComProspection> rechercherPreProspectionsParNom(@Param("motif") String motif,
                                                          @Param("limite") int limite);

    /**
     * Variante de {@link #rechercherPreProspectionsParNom} restreinte au périmètre commercial.
     *
     * <p>Elle existe pour la même raison que sa jumelle par téléphone, et l'omettre serait un
     * trou de sécurité plutôt qu'un oubli de confort : sans elle, la recherche par nom
     * deviendrait un contournement du périmètre — un commercial restreint verrait par le nom ce
     * que le numéro lui cache. Le filtre est <b>dans</b> la requête, jamais appliqué au résultat,
     * pour la raison expliquée sur {@link #findByNormalizedTempTelDansPerimetre}.
     */
    @Query(value = "SELECT TOP (:limite) p.* FROM ComProspection p "
            + "LEFT JOIN CfgTranche t ON t.PKey = p.StkHierarchyId "
            + "WHERE p.TempDescription COLLATE French_CI_AI LIKE :motif "
            + "AND (t.CfgProjectId IN (:projetIds) OR t.CfgProjectId IS NULL) "
            + "ORDER BY p.DocDate DESC",
            nativeQuery = true)
    List<ComProspection> rechercherPreProspectionsParNomDansPerimetre(@Param("motif") String motif,
                                                                       @Param("projetIds") List<UUID> projetIds,
                                                                       @Param("limite") int limite);

    /**
     * Doublons d'une pré-prospection qu'on vient de créer — {@code POST /api/pre-prospections},
     * voir {@code FEATURE-creation-pre-prospection.md} §3.
     *
     * <p>Même contrôle qu'OMRAN (nom + email + téléphone, trace d'audit du 21/09), rendu
     * tolérant : casse et espaces de bord ignorés, téléphone normalisé comme la hotline (9
     * derniers chiffres), et un champ absent des deux côtés compte comme égal — l'égalité SQL
     * stricte d'OMRAN ne se déclenche jamais dès qu'un des trois est {@code NULL}.
     *
     * <p>Les paramètres arrivent déjà normalisés : nom et email en minuscules sans espaces de
     * bord ({@code ""} si absents), téléphone par {@code PhoneNumberUtils.normalize}.
     */
    @Query("SELECT p FROM ComProspection p WHERE p.id <> :exclu "
            + "AND lower(trim(coalesce(p.tempDescription, ''))) = :nom "
            + "AND lower(trim(coalesce(p.tempEMail, ''))) = :email "
            + "AND right(replace(replace(replace(replace(replace(replace(coalesce(p.tempTel, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :telephone "
            + "ORDER BY p.userCreateDateTime DESC")
    List<ComProspection> trouverDoublonsContact(@Param("exclu") UUID exclu, @Param("nom") String nom,
                                                @Param("email") String email,
                                                @Param("telephone") String telephone, Pageable pageable);
}
