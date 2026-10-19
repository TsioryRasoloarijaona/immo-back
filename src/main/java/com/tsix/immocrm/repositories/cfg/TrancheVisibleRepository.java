package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/** Tranche lue par id sous la règle de visibilité par entité ({@code VisibiliteEntites.CLAUSE_JPQL}). */
public interface TrancheVisibleRepository extends JpaRepository<CfgTranche, UUID> {

    /** Partagée avec {@link CfgTrancheRepository#trancheVisible} : une seule formulation de la règle. */
    String TRANCHE_VISIBLE = "SELECT t FROM CfgTranche t LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc "
            + "WHERE t.id = :id AND " + VisibiliteEntites.CLAUSE_JPQL;

    @Query(TRANCHE_VISIBLE)
    Optional<CfgTranche> trancheVisible(UUID id);
}
