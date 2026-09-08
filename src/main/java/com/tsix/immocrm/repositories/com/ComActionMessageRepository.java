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

    /**
     * Agrégat d'activité par commercial pour {@code GET /api/kpi/commerciaux[/moi]} — une ligne par
     * commercial, colonnes dans l'ordre : {@code commercialId}, appels sortants, appels entrants,
     * visites consignées, visites décrochées, rendez-vous pris, interactions totales, prospections
     * distinctes travaillées.
     *
     * <p><b>Le regroupement se fait sur l'employé du compte auteur</b>
     * ({@code ComActionMessage.AdmUserId} → {@code AdmUser.CfgEmployeeId}), jamais sur
     * {@code ComActionMessage.CfgEmployeeId} : cette dernière n'est renseignée qu'à 77 % et désigne
     * une personne différente sur 844 lignes réelles — c'est l'employé <i>concerné</i>, pas
     * l'auteur. Voir {@code FEATURE-kpi-performance-commerciaux.md} §3 et la doc du champ.
     *
     * <p>Regrouper par employé et non par compte est délibéré : un employé porte parfois deux
     * comptes, dont les chiffres doivent s'additionner sur une seule ligne.
     *
     * <p>La visibilité par entité résout la société <b>prospection d'abord, dossier ensuite</b>
     * ({@code COALESCE}), même ordre que {@link #findEvenementsVisibles} et que
     * {@code VisibiliteEntites.depuisDeuxProjets} — sans quoi la même interaction serait rattachée
     * à des sociétés différentes selon l'écran. Une interaction ne résolvant aucune société reste
     * comptée (R2).
     */
    @Query("SELECT u.cfgEmployee.id, "
            + "SUM(CASE WHEN mt.code = :typeAppelSortant THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN mt.code = :typeAppelEntrant THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN mt.code = :typeVisite THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN sc.code = :resultatVisiteDecrochee THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN sc.code = :resultatRdvPris THEN 1 ELSE 0 END), "
            + "COUNT(m), COUNT(DISTINCT p.id) "
            + "FROM ComActionMessage m JOIN m.admUser u "
            + "LEFT JOIN m.comActionMessageType mt LEFT JOIN m.comActionMessageSubCategory sc "
            + "LEFT JOIN m.comProspection p LEFT JOIN p.stkHierarchy tp LEFT JOIN tp.cfgProject prp "
            + "LEFT JOIN prp.cfgCompany socp "
            + "LEFT JOIN m.comFolder f LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy tf "
            + "LEFT JOIN tf.cfgProject prf LEFT JOIN prf.cfgCompany socf "
            + "WHERE m.docDate BETWEEN :debut AND :fin AND u.cfgEmployee.id IS NOT NULL "
            + "AND (COALESCE(socp.isActive, socf.isActive) = true "
            + "     OR COALESCE(socp.id, socf.id) IS NULL) "
            + "GROUP BY u.cfgEmployee.id")
    List<Object[]> agregerActiviteParCommercial(@Param("debut") LocalDateTime debut,
                                                @Param("fin") LocalDateTime fin,
                                                @Param("typeAppelSortant") String typeAppelSortant,
                                                @Param("typeAppelEntrant") String typeAppelEntrant,
                                                @Param("typeVisite") String typeVisite,
                                                @Param("resultatVisiteDecrochee") String resultatVisiteDecrochee,
                                                @Param("resultatRdvPris") String resultatRdvPris);

    /**
     * Variante de {@link #agregerActiviteParCommercial} restreinte au périmètre commercial de
     * l'appelant. Même {@code COALESCE} prospection-puis-dossier pour le projet, et même règle
     * §6.4 : une interaction dont aucun projet ne se résout reste comptée.
     */
    @Query("SELECT u.cfgEmployee.id, "
            + "SUM(CASE WHEN mt.code = :typeAppelSortant THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN mt.code = :typeAppelEntrant THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN mt.code = :typeVisite THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN sc.code = :resultatVisiteDecrochee THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN sc.code = :resultatRdvPris THEN 1 ELSE 0 END), "
            + "COUNT(m), COUNT(DISTINCT p.id) "
            + "FROM ComActionMessage m JOIN m.admUser u "
            + "LEFT JOIN m.comActionMessageType mt LEFT JOIN m.comActionMessageSubCategory sc "
            + "LEFT JOIN m.comProspection p LEFT JOIN p.stkHierarchy tp LEFT JOIN tp.cfgProject prp "
            + "LEFT JOIN prp.cfgCompany socp "
            + "LEFT JOIN m.comFolder f LEFT JOIN f.stkItem i LEFT JOIN i.stkHierarchy tf "
            + "LEFT JOIN tf.cfgProject prf LEFT JOIN prf.cfgCompany socf "
            + "WHERE m.docDate BETWEEN :debut AND :fin AND u.cfgEmployee.id IS NOT NULL "
            + "AND (COALESCE(socp.isActive, socf.isActive) = true "
            + "     OR COALESCE(socp.id, socf.id) IS NULL) "
            + "AND (COALESCE(prp.id, prf.id) IN :projetIds OR COALESCE(prp.id, prf.id) IS NULL) "
            + "GROUP BY u.cfgEmployee.id")
    List<Object[]> agregerActiviteParCommercialDansPerimetre(@Param("debut") LocalDateTime debut,
                                                             @Param("fin") LocalDateTime fin,
                                                             @Param("typeAppelSortant") String typeAppelSortant,
                                                             @Param("typeAppelEntrant") String typeAppelEntrant,
                                                             @Param("typeVisite") String typeVisite,
                                                             @Param("resultatVisiteDecrochee") String resultatVisiteDecrochee,
                                                             @Param("resultatRdvPris") String resultatRdvPris,
                                                             @Param("projetIds") List<UUID> projetIds);
}
