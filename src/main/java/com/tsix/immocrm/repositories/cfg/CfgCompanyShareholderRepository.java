package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgCompanyShareholder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CfgCompanyShareholderRepository extends JpaRepository<CfgCompanyShareholder, UUID> {

    List<CfgCompanyShareholder> findByCfgCompany_IdOrderByCfgShareholder_CodeAsc(UUID entiteId);

    boolean existsByCfgShareholder_Id(UUID actionnaireId);
}
