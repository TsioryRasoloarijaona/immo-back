package com.tsix.immocrm.repositories.act;

import com.tsix.immocrm.entities.act.ActAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActAccountRepository extends JpaRepository<ActAccount, UUID> {

    /** Tous les comptes triés par code, comme le sélecteur « Plan compt. » de la fiche tranche OMRAN. */
    List<ActAccount> findAllByOrderByCodeAsc();
}
