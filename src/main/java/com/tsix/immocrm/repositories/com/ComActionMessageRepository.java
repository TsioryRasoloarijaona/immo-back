package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComActionMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<ComActionMessage> findByComProspectionIdOrderByDocDateDesc(UUID prospectionId, Pageable pageable);

    /**
     * {@code GET /api/hotline/recherche} — every interaction ({@code ComActionMessage}) logged
     * directly against a {@code CfgTier} (as opposed to one scoped to a single prospection or
     * dossier), most recent first.
     */
    Page<ComActionMessage> findByCfgTierIdOrderByDocDateDesc(UUID tierId, Pageable pageable);

    /**
     * {@code GET /api/calendrier/evenements} — every interaction that carries a calendar entry
     * ({@code AdmEvent}, created when the "Résultat" is a rendez-vous or a planned visit — see
     * {@code ProspectionService.RESULTATS_AVEC_DATE_PLANIFIEE}) whose {@code AdmEvent.StartHour}
     * falls within [debut, fin], earliest first.
     *
     * <p>Restreint aux entités juridiques en exploitation
     * ({@code instructions/FEATURE-visibilite-transversale-entites.md}). La société se résout par
     * <b>deux chemins</b>, prospection d'abord puis dossier — même ordre de {@code coalesce} que
     * {@code VisibiliteEntites.depuisDeuxProjets}, pour que l'agenda et la recherche hotline
     * désignent toujours la même société. Un événement rattaché ni à une prospection ni à un
     * dossier reste affiché (R2).
     *
     * <p>Écrit en JPQL plutôt qu'en méthode dérivée : le nom de méthode dérivée ne peut pas porter
     * ce prédicat, et une jointure interne sur la société masquerait les événements non résolus.
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
}
