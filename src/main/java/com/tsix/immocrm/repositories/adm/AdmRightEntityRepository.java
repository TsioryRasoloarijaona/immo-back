package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmRightEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

/** Lecture seule : aucune méthode d'écriture exposée. */
public interface AdmRightEntityRepository extends Repository<AdmRightEntity, UUID> {

    List<AdmRightEntity> findByCodeIsNotNullOrderBySortingAscCodeAsc();
}
