package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmSpecificField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmSpecificFieldRepository extends JpaRepository<AdmSpecificField, UUID> {

    /** Toutes les lignes d'entité du code comptent (doublons OMRAN). */
    @Query("SELECT f FROM AdmSpecificField f JOIN f.entite e LEFT JOIN FETCH f.dataType WHERE e.code = :code "
            + "ORDER BY f.categorySorting ASC, f.category ASC, f.sorting ASC")
    List<AdmSpecificField> duPorteur(String code);

    @Query("SELECT f FROM AdmSpecificField f JOIN f.entite e LEFT JOIN FETCH f.dataType WHERE f.id = :id AND e.code = :code")
    Optional<AdmSpecificField> duPorteur(UUID id, String code);
}
