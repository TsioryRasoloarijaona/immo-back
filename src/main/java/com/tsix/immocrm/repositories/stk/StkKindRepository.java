package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StkKindRepository extends JpaRepository<StkKind, UUID> {

    /** Nature par code ({@code BAT} = Bâtiment) : jamais le GUID en dur. */
    Optional<StkKind> findFirstByCode(String code);
}
