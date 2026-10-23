package com.tsix.immocrm.repositories.lnd;

import com.tsix.immocrm.entities.lnd.LndLandZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LndLandZoneRepository extends JpaRepository<LndLandZone, UUID> {

    List<LndLandZone> findByProspectionIdOrderByIdentificationAsc(UUID prospectionId);
}
