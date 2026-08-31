package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmFamilySituation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmFamilySituationRepository extends JpaRepository<AdmFamilySituation, UUID> {
    List<AdmFamilySituation> findAllByOrderBySortingAsc();
}
