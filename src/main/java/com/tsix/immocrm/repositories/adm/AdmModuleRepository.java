package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmModule;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

/** {@link Repository} et non {@code JpaRepository} : référentiel du légataire, jamais écrit par nous. */
public interface AdmModuleRepository extends Repository<AdmModule, UUID> {

    List<AdmModule> findByCode(String code);
}
