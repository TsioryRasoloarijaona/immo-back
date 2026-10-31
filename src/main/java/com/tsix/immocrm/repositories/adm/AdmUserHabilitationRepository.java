package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Écritures limitées aux lignes de portée projet : les autres appartiennent au logiciel de bureau. */
public interface AdmUserHabilitationRepository extends JpaRepository<AdmUserHabilitation, UUID> {

    /** Liste vide = aucune restriction (opt-in, FEATURE-perimetre-commercial.md §6.3). */
    @Query("SELECT h.cfgProject.id FROM AdmUserHabilitation h "
            + "WHERE h.admUser.id = :admUserId AND h.cfgProject IS NOT NULL")
    List<UUID> findProjetIdsByAdmUserId(@Param("admUserId") UUID admUserId);

    /** Tranches habilitées (portée tranche, comme OMRAN {@code CommercialList.cs} l.158). */
    @Query("SELECT h.cfgTranche.id FROM AdmUserHabilitation h "
            + "WHERE h.admUser.id = :admUserId AND h.cfgTranche IS NOT NULL")
    List<UUID> findTrancheIdsByAdmUserId(@Param("admUserId") UUID admUserId);

    /** Le seul sous-ensemble que {@code PUT /api/utilisateurs/{admUserId}/projets} peut supprimer. */
    List<AdmUserHabilitation> findByAdmUserIdAndCfgProjectIsNotNull(UUID admUserId);

    @Query("SELECT p.id, p.code, p.description FROM AdmUserHabilitation h JOIN h.cfgProject p "
            + "WHERE h.admUser.id = :admUserId ORDER BY p.code")
    List<Object[]> findProjetsByAdmUserId(@Param("admUserId") UUID admUserId);

    /** Tuples [admUserId, projetId] des lignes de portée projet de ces comptes. */
    @Query("SELECT h.admUser.id, h.cfgProject.id FROM AdmUserHabilitation h "
            + "WHERE h.admUser.id IN :admUserIds AND h.cfgProject IS NOT NULL")
    List<Object[]> findProjetIdsByAdmUserIdIn(@Param("admUserIds") List<UUID> admUserIds);

    /** Lignes de portée projet d'un compte sur ces projets (retrait par l'écran « Mon équipe »). */
    @Query("SELECT h FROM AdmUserHabilitation h "
            + "WHERE h.admUser.id = :admUserId AND h.cfgProject.id IN :projetIds")
    List<AdmUserHabilitation> findByAdmUserIdAndProjetIdIn(@Param("admUserId") UUID admUserId,
                                                           @Param("projetIds") List<UUID> projetIds);

    /** Tuple : [admUserId, nombreProjets]. */
    @Query("SELECT h.admUser.id, COUNT(h) FROM AdmUserHabilitation h "
            + "WHERE h.admUser.id IN :admUserIds AND h.cfgProject IS NOT NULL GROUP BY h.admUser.id")
    List<Object[]> countProjetsGroupedByAdmUserId(@Param("admUserIds") List<UUID> admUserIds);

    List<AdmUserHabilitation> findByAdmUserId(UUID admUserId);

    /** Arbre de l'écran : tuples [id, libellé, parent], sans filtre d'activité ; une société est son propre parent. */
    @Query(value = "SELECT PKey, COALESCE(NULLIF(Description, ''), Code), PKey FROM CfgCompany ORDER BY Code ASC",
            nativeQuery = true)
    List<Object[]> noeudsSocietes();

    @Query(value = "SELECT PKey, COALESCE(NULLIF(Description, ''), Code), CfgCompanyId FROM CfgProject "
            + "WHERE CfgCompanyId IS NOT NULL ORDER BY Code ASC", nativeQuery = true)
    List<Object[]> noeudsProjets();

    @Query(value = "SELECT PKey, COALESCE(NULLIF(Description, ''), Code), CfgProjectId FROM CfgTranche "
            + "WHERE CfgProjectId IS NOT NULL ORDER BY Code ASC", nativeQuery = true)
    List<Object[]> noeudsTranches();

    @Query(value = "SELECT PKey, COALESCE(NULLIF(Description, ''), Code), CfgCompanyId FROM PrjProject "
            + "WHERE CfgCompanyId IS NOT NULL ORDER BY Code ASC", nativeQuery = true)
    List<Object[]> noeudsProjetsTravaux();
}
