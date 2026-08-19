package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmCountry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmCountryRepository extends JpaRepository<AdmCountry, UUID> {
    List<AdmCountry> findAllByOrderByDescriptionAsc();
}
