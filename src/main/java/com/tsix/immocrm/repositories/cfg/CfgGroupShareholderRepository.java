package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CfgGroupShareholderRepository extends JpaRepository<CfgGroupShareholder, UUID> {

    Optional<CfgGroupShareholder> findFirstByCode(String code);
}
