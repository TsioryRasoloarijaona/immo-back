package com.tsix.immocrm.repositories.app;

import com.tsix.immocrm.entities.app.AppApiClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AppApiClientRepository extends JpaRepository<AppApiClient, UUID> {

    /** Résolution d'une clé à chaque requête : par le hash, unique. */
    Optional<AppApiClient> findByCleHash(String cleHash);

    boolean existsByNomIgnoreCase(String nom);

    /**
     * Trace du dernier usage, écrite au plus une fois par minute par le filtre — une mise à
     * jour ciblée, hors du contexte de persistance de la requête métier.
     */
    @Modifying
    @Transactional
    @Query("UPDATE AppApiClient c SET c.dernierUsage = :quand WHERE c.id = :id")
    void toucher(@Param("id") UUID id, @Param("quand") LocalDateTime quand);
}
