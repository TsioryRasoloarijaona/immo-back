package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmMatrimonialRegime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmMatrimonialRegimeRepository extends JpaRepository<AdmMatrimonialRegime, UUID> {
    List<AdmMatrimonialRegime> findAllByOrderBySortingAsc();
}
