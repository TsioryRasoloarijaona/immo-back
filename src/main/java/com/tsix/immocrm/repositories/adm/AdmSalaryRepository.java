package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmSalary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmSalaryRepository extends JpaRepository<AdmSalary, UUID> {
    List<AdmSalary> findAllByOrderBySortingAsc();
}
