package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmSequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdmSequenceRepository extends JpaRepository<AdmSequence, UUID> {

    Optional<AdmSequence> findByCode(String code);
}
