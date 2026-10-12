package com.tsix.immocrm.repositories.prj;

import com.tsix.immocrm.entities.prj.PrjProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrjProjectRepository extends JpaRepository<PrjProject, UUID> {

    String AVEC_REFERENTIELS = "SELECT p FROM PrjProject p LEFT JOIN FETCH p.cfgCompany "
            + "LEFT JOIN FETCH p.strStatus LEFT JOIN FETCH p.cfgTierManager ";

    @Query(AVEC_REFERENTIELS)
    List<PrjProject> findAllAvecReferentiels();

    @Query(AVEC_REFERENTIELS + "WHERE p.id IN :ids")
    List<PrjProject> findAllAvecReferentiels(@Param("ids") Collection<UUID> ids);

    @Query(AVEC_REFERENTIELS + "WHERE p.id = :id")
    Optional<PrjProject> findAvecReferentiels(@Param("id") UUID id);

    /** Sélecteur « Projet de facturation » de la tranche ; entités actives seulement (une inactive → liste vide). */
    @Query("SELECT p FROM PrjProject p WHERE p.cfgCompany.isActive = true "
            + "AND (:entiteId IS NULL OR p.cfgCompany.id = :entiteId) ORDER BY p.description ASC")
    List<PrjProject> pourFacturation(@Param("entiteId") UUID entiteId);
}
