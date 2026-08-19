package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmProfession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmProfessionRepository extends JpaRepository<AdmProfession, UUID> {
    List<AdmProfession> findAllByOrderByDescriptionAsc();
}
