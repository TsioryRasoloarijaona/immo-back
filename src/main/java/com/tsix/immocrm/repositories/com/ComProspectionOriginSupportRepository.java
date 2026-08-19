package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspectionOriginSupport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComProspectionOriginSupportRepository extends JpaRepository<ComProspectionOriginSupport, UUID> {

    List<ComProspectionOriginSupport> findByComProspectionOriginId(UUID origineId);
}
