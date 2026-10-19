package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.UUID;

public interface AdmWebHistoricsRepository extends JpaRepository<AdmWebHistorics, UUID> {

    /** Lignes de l'objet et de ses enfants (comme OMRAN), limitées aux types donnés pour l'objet comme pour le parent. */
    @Query("SELECT h FROM AdmWebHistorics h WHERE (h.objectId = :objetId OR h.parentObjectId = :objetId) "
            + "AND h.objectType IN :types AND (h.parentObjectType IS NULL OR h.parentObjectType IN :types)")
    Page<AdmWebHistorics> journal(UUID objetId, Collection<String> types, Pageable pageable);
}
