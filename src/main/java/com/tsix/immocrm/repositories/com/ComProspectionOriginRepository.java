package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComProspectionOriginRepository extends JpaRepository<ComProspectionOrigin, UUID> {
}
