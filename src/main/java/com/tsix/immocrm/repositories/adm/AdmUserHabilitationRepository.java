package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Accès à {@code AdmUserHabilitation} (table légataire). Toutes les requêtes d'écriture de cette
 * feature sont volontairement restreintes aux lignes de portée <b>projet</b> — voir la doc de
 * l'entité pour la raison (les lignes société/tranche/PrjProject appartiennent à l'application de
 * bureau légataire et ne doivent jamais être touchées).
 */
public interface AdmUserHabilitationRepository extends JpaRepository<AdmUserHabilitation, UUID> {

    /**
     * Le périmètre projet d'un compte : les {@code CfgProjectId} non nuls de ses habilitations.
     * Une liste vide signifie "aucune restriction" (décision produit §6.3 de
     * {@code instructions/FEATURE-perimetre-commercial.md} : déploiement opt-in, compte par compte).
     */
    @Query("SELECT h.cfgProject.id FROM AdmUserHabilitation h "
            + "WHERE h.admUser.id = :admUserId AND h.cfgProject IS NOT NULL")
    List<UUID> findProjetIdsByAdmUserId(@Param("admUserId") UUID admUserId);

    /**
     * Les lignes de portée projet d'un compte — le <b>seul</b> sous-ensemble que
     * {@code PUT /api/utilisateurs/{admUserId}/projets} a le droit de supprimer.
     */
    List<AdmUserHabilitation> findByAdmUserIdAndCfgProjectIsNotNull(UUID admUserId);

    /** Les projets affectés, avec leur libellé, pour {@code GET /api/utilisateurs/{id}/projets}. */
    @Query("SELECT p.id, p.code, p.description FROM AdmUserHabilitation h JOIN h.cfgProject p "
            + "WHERE h.admUser.id = :admUserId ORDER BY p.code")
    List<Object[]> findProjetsByAdmUserId(@Param("admUserId") UUID admUserId);

    /**
     * Nombre de projets affectés par compte, pour la colonne {@code nombreProjets} de
     * {@code GET /api/utilisateurs}. Tuple : [admUserId, nombreProjets].
     */
    @Query("SELECT h.admUser.id, COUNT(h) FROM AdmUserHabilitation h "
            + "WHERE h.admUser.id IN :admUserIds AND h.cfgProject IS NOT NULL GROUP BY h.admUser.id")
    List<Object[]> countProjetsGroupedByAdmUserId(@Param("admUserIds") List<UUID> admUserIds);
}
