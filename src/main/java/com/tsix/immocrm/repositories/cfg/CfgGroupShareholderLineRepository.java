package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgGroupShareholderLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CfgGroupShareholderLineRepository extends JpaRepository<CfgGroupShareholderLine, UUID> {

    /** Ordre de {@code SetRepartitions} : code d'actionnaire croissant, la dernière ligne reçoit le reste. */
    List<CfgGroupShareholderLine> findByCfgGroupShareholder_IdOrderByCfgShareholder_CodeAsc(UUID groupeId);

    boolean existsByCfgShareholder_Id(UUID actionnaireId);
}
