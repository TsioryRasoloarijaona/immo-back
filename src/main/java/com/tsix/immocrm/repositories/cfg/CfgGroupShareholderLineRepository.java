package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgGroupShareholderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CfgGroupShareholderLineRepository extends JpaRepository<CfgGroupShareholderLine, UUID> {

    /** Ordre stable (code de l'actionnaire, puis id) : la dernière ligne reçoit le reste dans {@code SetRepartitions}. */
    @Query("SELECT l FROM CfgGroupShareholderLine l LEFT JOIN l.cfgShareholder s "
            + "WHERE l.cfgGroupShareholder.id = :groupeId ORDER BY s.code ASC, l.id ASC")
    List<CfgGroupShareholderLine> duGroupe(@Param("groupeId") UUID groupeId);

    List<CfgGroupShareholderLine> findByCfgShareholderId(UUID actionnaireId);
}
