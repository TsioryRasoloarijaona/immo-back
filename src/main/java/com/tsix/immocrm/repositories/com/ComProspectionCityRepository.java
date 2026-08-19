package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspectionCity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComProspectionCityRepository extends JpaRepository<ComProspectionCity, UUID> {

    List<ComProspectionCity> findByComProspectionId(UUID prospectionId);

    void deleteByIdAndComProspectionId(UUID id, UUID prospectionId);
}
