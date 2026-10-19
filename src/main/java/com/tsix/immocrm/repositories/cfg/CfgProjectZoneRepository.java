package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgProjectZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfgProjectZoneRepository extends JpaRepository<CfgProjectZone, UUID> {

    List<CfgProjectZone> findByCfgProjectIdOrderBySortingAscCodeAsc(UUID projetId);

    Optional<CfgProjectZone> findByIdAndCfgProjectId(UUID id, UUID projetId);

    long countByParentId(UUID parentId);

    /** Unicité OMRAN Code + ParentId + CfgProjectId ; {@code ParentId} nul compare à nul. */
    @Query("SELECT COUNT(z) FROM CfgProjectZone z WHERE z.cfgProject.id = :projetId AND z.code = :code "
            + "AND ((:parentId IS NULL AND z.parentId IS NULL) OR z.parentId = :parentId) AND z.id <> :id")
    long doublons(@Param("projetId") UUID projetId, @Param("parentId") UUID parentId, @Param("code") String code,
                  @Param("id") UUID id);
}
