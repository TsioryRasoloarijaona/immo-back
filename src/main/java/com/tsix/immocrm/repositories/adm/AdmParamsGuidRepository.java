package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmParamsGuid;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/** {@link Repository} et non {@code JpaRepository} : paramètres du légataire, jamais écrits par nous. */
public interface AdmParamsGuidRepository extends Repository<AdmParamsGuid, String> {

    Optional<AdmParamsGuid> findById(String code);
}
