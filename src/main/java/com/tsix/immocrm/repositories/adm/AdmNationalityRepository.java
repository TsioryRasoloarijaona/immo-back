package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmNationality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmNationalityRepository extends JpaRepository<AdmNationality, UUID> {
    List<AdmNationality> findAllByOrderByDescriptionAsc();
}
