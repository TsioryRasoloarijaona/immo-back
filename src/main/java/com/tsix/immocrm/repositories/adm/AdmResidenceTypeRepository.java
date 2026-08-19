package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmResidenceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmResidenceTypeRepository extends JpaRepository<AdmResidenceType, UUID> {
    List<AdmResidenceType> findAllByOrderBySortingAsc();
}
