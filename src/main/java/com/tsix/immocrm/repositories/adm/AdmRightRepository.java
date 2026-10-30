package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmRight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmRightRepository extends JpaRepository<AdmRight, UUID> {

    List<AdmRight> findByAdmRoleId(UUID admRoleId);
}
