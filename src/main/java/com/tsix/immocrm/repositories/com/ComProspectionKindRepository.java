package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspectionKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComProspectionKindRepository extends JpaRepository<ComProspectionKind, UUID> {
}
