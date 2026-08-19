package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmSexType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmSexTypeRepository extends JpaRepository<AdmSexType, UUID> {
    List<AdmSexType> findAllByOrderBySortingAsc();
}
