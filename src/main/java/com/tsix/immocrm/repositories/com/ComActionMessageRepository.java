package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComActionMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ComActionMessageRepository extends JpaRepository<ComActionMessage, UUID>,
        JpaSpecificationExecutor<ComActionMessage> {

    List<ComActionMessage> findByComProspectionIdOrderByDocDateDesc(UUID prospectionId);

    /** Ce que le bureau désarme quand une nouvelle alerte est posée — voir {@code AlerteRelanceService}. */
    List<ComActionMessage> findByComProspectionIdAndAlertValidityDateIsNotNullAndComDocumentIsNull(UUID prospectionId);

    /** Compte auteur et son employé chargés avec la page : {@code ProspectionService.nomAuteur} les lit par ligne. */
    @EntityGraph(attributePaths = {"admUser", "admUser.cfgEmployee"})
    Page<ComActionMessage> findByComProspectionIdOrderByDocDateDesc(UUID prospectionId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"admUser", "admUser.cfgEmployee"})
    Page<ComActionMessage> findAll(Specification<ComActionMessage> spec, Pageable pageable);

    Page<ComActionMessage> findByCfgTierIdOrderByDocDateDesc(UUID tierId, Pageable pageable);

    /**
     * Société résolue prospection d'abord, dossier ensuite — même ordre que
     * {@code VisibiliteEntites.depuisDeuxProjets}. Jointures {@code LEFT} : une jointure interne masquerait
     * les événements sans société, qui restent visibles (FEATURE-visibilite-transversale-entites.md R2).
     */
    @Query("SELECT m FROM ComActionMessage m JOIN m.admEvent e "
            + "LEFT JOIN m.comProspection p LEFT JOIN p.stkHierarchy tp LEFT JOIN tp.cfgProject prp "
            + "LEFT JOIN prp.cfgCompany socp "
            + "LEFT JOIN m.comFolder f LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy tf "
            + "LEFT JOIN tf.cfgProject prf LEFT JOIN prf.cfgCompany socf "
            + "WHERE e.startHour BETWEEN :debut AND :fin "
            + "AND (COALESCE(socp.isActive, socf.isActive) = true "
            + "     OR COALESCE(socp.id, socf.id) IS NULL) "
            + "ORDER BY e.startHour ASC")
    List<ComActionMessage> findEvenementsVisibles(@Param("debut") LocalDateTime debut,
                                                  @Param("fin") LocalDateTime fin);

    /**
     * Une ligne par action (compte, employé, {@code UserName}, type, libellé, résultat, prospection) : un
     * {@code GROUP BY} ne sait pas exprimer l'attribution par nom (FEATURE-suivi-actions.md §3bis et §7). Actions
     * « Système » exclues (non attribuées par OMRAN) ; société prospection puis dossier, non résolue = comptée.
     */
    @Query("SELECT u.id, ue.id, m.userName, mt.code, mt.description, sc.code, p.id "
            + "FROM ComActionMessage m LEFT JOIN m.admUser u LEFT JOIN u.cfgEmployee ue "
            + "LEFT JOIN m.comActionMessageType mt LEFT JOIN m.comActionMessageSubCategory sc "
            + "LEFT JOIN m.comProspection p LEFT JOIN p.stkHierarchy tp LEFT JOIN tp.cfgProject prp "
            + "LEFT JOIN prp.cfgCompany socp "
            + "LEFT JOIN m.comFolder f LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy tf "
            + "LEFT JOIN tf.cfgProject prf LEFT JOIN prf.cfgCompany socf "
            + "WHERE m.docDate BETWEEN :debut AND :fin "
            + "AND (m.userName IS NULL OR m.userName <> :systeme) "
            + "AND (COALESCE(socp.isActive, socf.isActive) = true "
            + "     OR COALESCE(socp.id, socf.id) IS NULL)")
    List<Object[]> projeterActionsPourKpi(@Param("debut") LocalDateTime debut,
                                          @Param("fin") LocalDateTime fin,
                                          @Param("systeme") String systeme);

    /** Même requête, dans le périmètre ; une action sans projet résolu reste comptée (FEATURE-perimetre-commercial.md §6.4). */
    @Query("SELECT u.id, ue.id, m.userName, mt.code, mt.description, sc.code, p.id "
            + "FROM ComActionMessage m LEFT JOIN m.admUser u LEFT JOIN u.cfgEmployee ue "
            + "LEFT JOIN m.comActionMessageType mt LEFT JOIN m.comActionMessageSubCategory sc "
            + "LEFT JOIN m.comProspection p LEFT JOIN p.stkHierarchy tp LEFT JOIN tp.cfgProject prp "
            + "LEFT JOIN prp.cfgCompany socp "
            + "LEFT JOIN m.comFolder f LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy tf "
            + "LEFT JOIN tf.cfgProject prf LEFT JOIN prf.cfgCompany socf "
            + "WHERE m.docDate BETWEEN :debut AND :fin "
            + "AND (m.userName IS NULL OR m.userName <> :systeme) "
            + "AND (COALESCE(socp.isActive, socf.isActive) = true "
            + "     OR COALESCE(socp.id, socf.id) IS NULL) "
            + "AND (COALESCE(prp.id, prf.id) IN :projetIds OR COALESCE(prp.id, prf.id) IS NULL)")
    List<Object[]> projeterActionsPourKpiDansPerimetre(@Param("debut") LocalDateTime debut,
                                                       @Param("fin") LocalDateTime fin,
                                                       @Param("systeme") String systeme,
                                                       @Param("projetIds") List<UUID> projetIds);

    /**
     * Dernière alerte par prospection en une requête pour toute la page, depuis les interactions
     * ({@code ComProspection.AlertValidityDate} est morte). Dernière = {@code DocDate} le plus récent ;
     * {@code WithoutRevival = 1} = désarmée. Jamais avec une liste vide : {@code IN ()} est invalide.
     */
    @Query(value = "SELECT a.ComProspectionId AS prospectionId, a.AlertValidityDate AS dateAlerte, "
            + "CAST(a.AlertMessage AS nvarchar(max)) AS message FROM ("
            + "SELECT m.ComProspectionId, m.AlertValidityDate, m.AlertMessage, "
            + "ROW_NUMBER() OVER (PARTITION BY m.ComProspectionId "
            + "ORDER BY m.DocDate DESC, m.AlertValidityDate DESC) AS rn "
            + "FROM ComActionMessage m "
            + "WHERE m.ComProspectionId IN (:prospectionIds) AND m.AlertValidityDate IS NOT NULL "
            + "AND ISNULL(m.WithoutRevival, 0) = 0"
            + ") a WHERE a.rn = 1",
            nativeQuery = true)
    List<DerniereAlerteProjection> dernieresAlertes(@Param("prospectionIds") List<UUID> prospectionIds);

    /** Les noms doivent suivre les alias de {@link #dernieresAlertes}. */
    interface DerniereAlerteProjection {
        UUID getProspectionId();

        LocalDateTime getDateAlerte();

        String getMessage();
    }
}
