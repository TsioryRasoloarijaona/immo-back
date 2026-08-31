package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AdmCityRepository extends JpaRepository<AdmCity, UUID>, JpaSpecificationExecutor<AdmCity> {
}
