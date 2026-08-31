package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StlAccountRepository extends JpaRepository<StlAccount, UUID> {

    /** {@code GET /api/referentiels/comptes} — pas de colonne Sorting, tri alphabétique. */
    List<StlAccount> findAllByOrderByDescriptionAsc();
}
