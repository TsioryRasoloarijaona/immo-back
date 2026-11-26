package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTierContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfgTierContactRepository extends JpaRepository<CfgTierContact, UUID> {

    List<CfgTierContact> findByCfgTierIdOrderByUserCreateDateTimeDesc(UUID tierId);

    void deleteByIdAndCfgTierId(UUID id, UUID tierId);

    boolean existsByCfgTierIdAndCfgContactId(UUID tierId, UUID contactId);

    Optional<CfgTierContact> findByIdAndCfgTierId(UUID id, UUID tierId);

    boolean existsByCfgContactIdAndCfgRelationTypeCodeAndCfgTierIdNot(UUID contactId, String codeRelation, UUID tierId);

    /** Tiers liés au contact par ce type de lien (agence d'un vendeur). */
    @Query("""
            select l.cfgTier.id from CfgTierContact l
            where l.cfgContact.id = :contactId and l.cfgRelationType.code = :codeRelation order by l.cfgTier.id""")
    List<UUID> tiersDuContact(@Param("contactId") UUID contactId, @Param("codeRelation") String codeRelation);

    /** Contacts liés au tiers par ce type de lien (vendeurs d'une agence). */
    @Query("""
            select l.cfgContact.id from CfgTierContact l
            where l.cfgTier.id = :tierId and l.cfgRelationType.code = :codeRelation and l.cfgContact is not null""")
    List<UUID> contactsDuTiers(@Param("tierId") UUID tierId, @Param("codeRelation") String codeRelation);
}
