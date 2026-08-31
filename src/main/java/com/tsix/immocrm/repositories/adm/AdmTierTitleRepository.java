package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmTierTitle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmTierTitleRepository extends JpaRepository<AdmTierTitle, UUID> {
    List<AdmTierTitle> findAllByOrderBySortingAsc();
}
