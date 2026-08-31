package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspectionSelectedProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComProspectionSelectedProfileRepository extends JpaRepository<ComProspectionSelectedProfile, UUID> {

    List<ComProspectionSelectedProfile> findByComProspectionIdOrderByUserCreateDateTimeDesc(UUID prospectionId);

    void deleteByIdAndComProspectionId(UUID id, UUID prospectionId);
}
