package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlAccountPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface StlAccountPositionRepository extends JpaRepository<StlAccountPosition, UUID> {

    /** Dernier rapprochement du compte ({@code Account.cs:GetLastPostionDate}) ; vide s'il n'en a aucun. */
    @Query("SELECT MAX(p.positionDate) FROM StlAccountPosition p WHERE p.stlAccountId = :compteId")
    Optional<LocalDateTime> dernierRapprochement(UUID compteId);
}
