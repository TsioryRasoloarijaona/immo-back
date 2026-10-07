package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdmRoleRepository extends JpaRepository<AdmRole, UUID> {

}
