package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgCompanyShareholder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CfgCompanyShareholderRepository extends JpaRepository<CfgCompanyShareholder, UUID> {

    @Query("SELECT l FROM CfgCompanyShareholder l LEFT JOIN l.cfgShareholder s "
            + "WHERE l.cfgCompany.id = :entiteId ORDER BY s.code ASC, l.id ASC")
    List<CfgCompanyShareholder> deLEntite(@Param("entiteId") UUID entiteId);

    long countByCfgShareholderId(UUID actionnaireId);
}
