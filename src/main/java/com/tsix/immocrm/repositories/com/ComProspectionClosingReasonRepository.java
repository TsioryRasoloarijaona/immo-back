package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspectionClosingReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComProspectionClosingReasonRepository extends JpaRepository<ComProspectionClosingReason, UUID> {
}
