package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdmEventRepository extends JpaRepository<AdmEvent, UUID> {
}
