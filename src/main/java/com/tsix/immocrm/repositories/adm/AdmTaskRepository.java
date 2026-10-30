package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface AdmTaskRepository extends JpaRepository<AdmTask, UUID>, JpaSpecificationExecutor<AdmTask> {

    Optional<AdmTask> findByIdAndStrEntityId(UUID id, UUID strEntityId);

    @Override
    @EntityGraph(attributePaths = {"type", "priorite", "statut", "declencheur", "gestionnaire", "assigne"})
    Page<AdmTask> findAll(Specification<AdmTask> spec, Pageable pageable);
}
