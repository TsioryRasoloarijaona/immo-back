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

    /** Hors pré-prospections ; une prospection sans tranche reste comptée (R2). */
    @Query("SELECT COUNT(p) FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE (p.strEntityId IS NULL OR p.strEntityId NOT IN (SELECT s.id FROM StrEntity s WHERE s.code = 'ComPreProspection')) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    long countExcludingPreProspections();

    @Query("SELECT COUNT(p) FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE (p.strEntityId IS NULL OR p.strEntityId NOT IN (SELECT s.id FROM StrEntity s WHERE s.code = 'ComPreProspection')) "
            + "AND (p.isClosed IS NULL OR p.isClosed = false) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    long countOpenExcludingPreProspections();

    /**
     * Projet par la tranche de la prospection, pas par {@code StkItem} (rare, n'ajoute rien :
     * FEATURE-perimetre-commercial.md). {@code pr.id IS NULL} inclus : sans projet, visible de tous (§6.4).
     */
    @Query("SELECT COUNT(p) FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE (p.strEntityId IS NULL OR p.strEntityId NOT IN (SELECT s.id FROM StrEntity s WHERE s.code = 'ComPreProspection')) "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    long countExcludingPreProspectionsDansPerimetre(@Param("projetIds") List<UUID> projetIds);

    @Query("SELECT COUNT(p) FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE (p.strEntityId IS NULL OR p.strEntityId NOT IN (SELECT s.id FROM StrEntity s WHERE s.code = 'ComPreProspection')) "
            + "AND (p.isClosed IS NULL OR p.isClosed = false) "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    long countOpenExcludingPreProspectionsDansPerimetre(@Param("projetIds") List<UUID> projetIds);

    /** Contrôle unique du périmètre par id ({@code PerimetreInterceptor}) : hors périmètre = 404, jamais 403. */
    @Query("SELECT COUNT(p) > 0 FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE p.id = :id AND (pr.id IN :projetIds OR pr.id IS NULL) "
            + "AND (soc.isActive = true OR soc.id IS NULL)")
    boolean existsDansPerimetre(@Param("id") UUID id, @Param("projetIds") List<UUID> projetIds);

    /** Appelant sans périmètre : seule la visibilité par entité (R4) ; sans tranche = accessible (R2). */
    @Query("SELECT COUNT(p) > 0 FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc "
            + "WHERE p.id = :id AND (soc.isActive = true OR soc.id IS NULL)")
    boolean existsVisible(@Param("id") UUID id);

    /**
     * [commercialId, docDate, isClosed, première interaction] par ligne : la médiane ne s'exprime pas en JPQL.
     * {@code cfgCommercial IS NOT NULL} exclut les pré-prospections (jamais de commercial). Le {@code MIN}
     * porte sur toutes les interactions : on mesure l'attente du prospect, pas le titulaire.
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

    Page<ComProspection> findByCfgProspectId(UUID prospectId, Pageable pageable);

    /** {@code TempTel} (saisi avant toute fiche), normalisé comme {@code CfgTierRepository#findByNormalizedPhone}. */
    @Query("SELECT p FROM ComProspection p WHERE "
            + "right(replace(replace(replace(replace(replace(replace(coalesce(p.tempTel, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :normalizedPhone")
    Page<ComProspection> findByNormalizedTempTel(@Param("normalizedPhone") String normalizedPhone, Pageable pageable);

    /**
     * Filtre dans la requête, jamais sur la page : la liste est plafonnée, filtrer après coup évincerait
     * les lignes du périmètre (FEATURE-perimetre-commercial.md §9ter.2).
     */
    @Query("SELECT p FROM ComProspection p LEFT JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr WHERE "
            + "right(replace(replace(replace(replace(replace(replace(coalesce(p.tempTel, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :normalizedPhone "
            + "AND (pr.id IN :projetIds OR pr.id IS NULL)")
    Page<ComProspection> findByNormalizedTempTelDansPerimetre(@Param("normalizedPhone") String normalizedPhone,
                                                               @Param("projetIds") List<UUID> projetIds,
                                                               Pageable pageable);

    /**
     * Par nom saisi ({@code TempDescription}, presque toujours renseigné). Native : la base est en
     * {@code French_CI_AS} (sensible aux accents) et JPQL ne sait pas exprimer {@code COLLATE}.
     */
    @Query(value = "SELECT TOP (:limite) p.* FROM ComProspection p "
            + "WHERE p.TempDescription COLLATE French_CI_AI LIKE :motif "
            + "ORDER BY p.DocDate DESC",
            nativeQuery = true)
    List<ComProspection> rechercherPreProspectionsParNom(@Param("motif") String motif,
                                                          @Param("limite") int limite);

    /**
     * Sans elle, le nom contournerait le périmètre que le numéro applique. Filtre dans la requête,
     * comme {@link #findByNormalizedTempTelDansPerimetre}.
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
     * Contrôle nom + email + téléphone d'OMRAN rendu tolérant : absent des deux côtés = égal (l'égalité
     * stricte d'OMRAN ne se déclenche jamais sur un NULL). Paramètres déjà normalisés, {@code ""} si absents
     * — voir FEATURE-creation-pre-prospection.md §3.
     */
    @Query("SELECT p FROM ComProspection p WHERE p.id <> :exclu "
            + "AND lower(trim(coalesce(p.tempDescription, ''))) = :nom "
            + "AND lower(trim(coalesce(p.tempEMail, ''))) = :email "
            + "AND right(replace(replace(replace(replace(replace(replace(coalesce(p.tempTel, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :telephone "
            + "ORDER BY p.userCreateDateTime DESC")
    List<ComProspection> trouverDoublonsContact(@Param("exclu") UUID exclu, @Param("nom") String nom,
                                                @Param("email") String email,
                                                @Param("telephone") String telephone, Pageable pageable);

    /**
     * Boîte de tri : autre ligne au même numéro (9 derniers chiffres) <b>ou</b> au même e-mail. Critères déjà
     * normalisés par {@code ProspectService.critereTelephone/critereEmail}, {@code ""} = critère absent.
     */
    @Query("SELECT p FROM ComProspection p WHERE p.id <> :exclu AND ("
            + "(:telephone <> '' AND right(replace(replace(replace(replace(replace(replace(coalesce(p.tempTel, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :telephone) "
            + "OR (:email <> '' AND lower(trim(coalesce(p.tempEMail, ''))) = :email)) "
            + "ORDER BY p.docDate DESC")
    List<ComProspection> trouverMemeTelephoneOuEmail(@Param("exclu") UUID exclu,
                                                     @Param("telephone") String telephone,
                                                     @Param("email") String email, Pageable pageable);

    /** Même contact que {@link #trouverDoublonsContact}, DocDate dans [debut, fin[ — sans plafond (réimport idempotent). */
    @Query("SELECT p FROM ComProspection p WHERE p.docDate >= :debut AND p.docDate < :fin "
            + "AND lower(trim(coalesce(p.tempDescription, ''))) = :nom "
            + "AND lower(trim(coalesce(p.tempEMail, ''))) = :email "
            + "AND right(replace(replace(replace(replace(replace(replace(coalesce(p.tempTel, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :telephone")
    List<ComProspection> trouverMemeSoumission(@Param("nom") String nom, @Param("email") String email,
                                               @Param("telephone") String telephone,
                                               @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /**
     * Règle « une prospection ouverte par prospect × projet » (FEATURE-doublons-prospect-projet.md §3) ;
     * ouverte = {@code IsClosed} 0 ou NULL. {@code exclu} : passer un id inexistant plutôt que {@code null},
     * un {@code <> NULL} ne retient rien.
     */
    @Query("SELECT p FROM ComProspection p JOIN p.stkHierarchy t "
            + "WHERE p.cfgProspect.id = :prospectId AND t.cfgProject.id = :projetId "
            + "AND p.id <> :exclu "
            + "AND (p.isClosed IS NULL OR p.isClosed = false) "
            + "AND (p.strEntityId IS NULL OR p.strEntityId NOT IN (SELECT s.id FROM StrEntity s WHERE s.code = 'ComPreProspection')) "
            + "ORDER BY p.docDate DESC")
    List<ComProspection> trouverOuvertesMemeProspectMemeProjet(@Param("prospectId") UUID prospectId,
                                                               @Param("projetId") UUID projetId,
                                                               @Param("exclu") UUID exclu);
}
