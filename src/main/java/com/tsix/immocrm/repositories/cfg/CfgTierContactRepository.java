package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTierContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfgTierContactRepository extends JpaRepository<CfgTierContact, UUID> {

    List<CfgTierContact> findByCfgTierIdOrderByUserCreateDateTimeDesc(UUID tierId);

    void deleteByIdAndCfgTierId(UUID id, UUID tierId);

    boolean existsByCfgTierIdAndCfgContactId(UUID tierId, UUID contactId);

    Optional<CfgTierContact> findByIdAndCfgTierId(UUID id, UUID tierId);
}
