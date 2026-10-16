package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StlAccountRepository extends JpaRepository<StlAccount, UUID> {

    /** Pas de colonne Sorting : tri alphabétique. */
    List<StlAccount> findAllByOrderByDescriptionAsc();

    List<StlAccount> findByCfgCompanyIdOrderByDescriptionAsc(UUID societeId);
}
