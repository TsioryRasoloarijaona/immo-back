package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgProjectZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfgProjectZoneRepository extends JpaRepository<CfgProjectZone, UUID> {

    @Query("SELECT z FROM CfgProjectZone z WHERE z.cfgProject.id = :projetId ORDER BY z.leveling ASC, z.sorting ASC, z.description ASC")
    List<CfgProjectZone> duProgramme(@Param("projetId") UUID projetId);

    Optional<CfgProjectZone> findByIdAndCfgProjectId(UUID id, UUID projetId);

    long countByCfgProjectIdAndParentId(UUID projetId, UUID parentId);

    long countByCfgProjectIdAndParentIdIsNull(UUID projetId);

    long countByParentId(UUID parentId);

    /** Unicité OMRAN Code + ParentId + CfgProjectId (collation French_CI_AS : « = » ignore la casse). */
    @Query("SELECT z FROM CfgProjectZone z WHERE z.cfgProject.id = :projetId AND z.code = :code AND z.id <> :id "
            + "AND ((:parentId IS NULL AND z.parentId IS NULL) OR z.parentId = :parentId)")
    List<CfgProjectZone> homonymes(@Param("projetId") UUID projetId, @Param("parentId") UUID parentId,
                                   @Param("code") String code, @Param("id") UUID id);
}
