package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgShareholder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CfgShareholderRepository extends JpaRepository<CfgShareholder, UUID> {

    Optional<CfgShareholder> findFirstByCode(String code);
}
