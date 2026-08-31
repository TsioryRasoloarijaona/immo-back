package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkPresentationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StkPresentationTypeRepository extends JpaRepository<StkPresentationType, UUID> {
}
