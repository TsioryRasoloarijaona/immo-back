package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmSpecificFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AdmSpecificFieldValueRepository extends JpaRepository<AdmSpecificFieldValue, UUID> {

    List<AdmSpecificFieldValue> findByNameSpaceId(UUID objetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AdmSpecificFieldValue v WHERE v.champId = :champId")
    void supprimerDuChamp(UUID champId);
}
