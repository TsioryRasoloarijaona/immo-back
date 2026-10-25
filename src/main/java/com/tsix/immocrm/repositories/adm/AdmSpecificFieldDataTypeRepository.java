package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmSpecificFieldDataType;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@link Repository} : référentiel OMRAN, jamais écrit par nous. */
public interface AdmSpecificFieldDataTypeRepository extends Repository<AdmSpecificFieldDataType, UUID> {

    Optional<AdmSpecificFieldDataType> findFirstByCode(String code);

    List<AdmSpecificFieldDataType> findAllByOrderBySortingAsc();
}
