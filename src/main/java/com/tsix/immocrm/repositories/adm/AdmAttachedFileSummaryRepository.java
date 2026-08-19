package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AdmAttachedFileSummaryRepository extends JpaRepository<AdmAttachedFileSummary, UUID> {

    /**
     * Pièces jointes of a Dossier — {@code CategoryId = ComFolder.PKey}, additionally filtered
     * on the "Dossier de vente" category prefix as a safety net since {@code CategoryId} is a
     * generic, unconstrained parent reference shared with other object types in this table
     * (see {@link com.tsix.immocrm.entities.adm.AdmAttachedFileSummary}).
     */
    @Query("SELECT a FROM AdmAttachedFileSummary a WHERE a.categoryId = :dossierId " +
            "AND a.category LIKE 'Dossier de vente%' ORDER BY a.userCreateDateTime DESC")
    List<AdmAttachedFileSummary> findByDossierId(UUID dossierId);
}
