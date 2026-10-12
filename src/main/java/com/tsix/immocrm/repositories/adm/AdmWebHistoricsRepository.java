package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AdmWebHistoricsRepository extends JpaRepository<AdmWebHistorics, UUID> {

    /**
     * Comme l'écran Audit d'OMRAN (l'objet et ses lignes enfants), mais seulement pour les {@code types} donnés :
     * la table journalise aussi prospections, factures… qu'un id ne doit pas rouvrir. Tri porté par le {@code Pageable}.
     */
    @Query("""
            SELECT h FROM AdmWebHistorics h
            WHERE (h.objectId = :id AND h.objectType IN :types)
               OR (h.parentObjectId = :id AND h.parentObjectType IN :types AND h.objectType IN :types)""")
    Page<AdmWebHistorics> duObjet(@Param("id") UUID objetId, @Param("types") Collection<String> types,
                                  Pageable pageable);

    List<AdmWebHistorics> findByObjectId(UUID objectId);
}
