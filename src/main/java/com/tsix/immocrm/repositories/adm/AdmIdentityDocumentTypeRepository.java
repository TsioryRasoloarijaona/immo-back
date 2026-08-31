package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmIdentityDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmIdentityDocumentTypeRepository extends JpaRepository<AdmIdentityDocumentType, UUID> {
    List<AdmIdentityDocumentType> findAllByOrderBySortingAsc();
}
