package com.tsix.immocrm.repositories.prj;

import com.tsix.immocrm.entities.prj.PrjProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PrjProjectRepository extends JpaRepository<PrjProject, UUID> {

    /** {@code entiteId} null = toutes les entités actives (PrjProjectItemList.GetList filtre sur l'entité). */
    @Query("SELECT p FROM PrjProject p WHERE p.cfgCompany.isActive = true "
            + "AND (:entiteId IS NULL OR p.cfgCompany.id = :entiteId) ORDER BY p.code")
    List<PrjProject> desEntitesActives(@Param("entiteId") UUID entiteId);
}
