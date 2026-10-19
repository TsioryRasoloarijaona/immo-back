package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/** Tranche lue par id sous la règle de visibilité par entité ({@code VisibiliteEntites.CLAUSE_JPQL}). */
public interface TrancheVisibleRepository extends JpaRepository<CfgTranche, UUID> {

    @Query("SELECT t FROM CfgTranche t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc "
            + "WHERE t.id = :id AND (soc.isActive = true OR soc.id IS NULL)")
    Optional<CfgTranche> trancheVisible(UUID id);

    /** Tranche visible, sinon 404 (inconnue ou d'une entité inactive). */
    default CfgTranche exigerVisible(UUID id) {
        return trancheVisible(id).orElseThrow(() -> new ResourceNotFoundException("Tranche introuvable: " + id));
    }
}
