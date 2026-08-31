package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspectionProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComProspectionProjectRepository extends JpaRepository<ComProspectionProject, UUID> {

    List<ComProspectionProject> findByComProspectionIdOrderByUserCreateDateTimeDesc(UUID prospectionId);

    void deleteByIdAndComProspectionId(UUID id, UUID prospectionId);
}
