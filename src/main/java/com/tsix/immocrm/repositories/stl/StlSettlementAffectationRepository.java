package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlSettlementAffectation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StlSettlementAffectationRepository extends JpaRepository<StlSettlementAffectation, UUID> {

    /**
     * Tuple : [stlSettlementId, montantAffecte (NULL sans ligne)]. Projection scalaire exprès : une entité déjà
     * chargée dans la session ne serait pas relue après l'ajout de lignes.
     */
    @Query("SELECT a.id, a.montantAffecte FROM StlSettlementAffectation a WHERE a.id IN :reglementIds")
    List<Object[]> montantsAffectes(Collection<UUID> reglementIds);
}
