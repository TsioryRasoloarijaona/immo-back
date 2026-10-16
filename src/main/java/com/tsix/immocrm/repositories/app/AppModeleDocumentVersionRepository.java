package com.tsix.immocrm.repositories.app;

import com.tsix.immocrm.entities.app.AppModeleDocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppModeleDocumentVersionRepository extends JpaRepository<AppModeleDocumentVersion, UUID> {

    List<AppModeleDocumentVersion> findByModeleIdOrderByNumeroDesc(UUID modeleId);

    Optional<AppModeleDocumentVersion> findByModeleIdAndNumero(UUID modeleId, Integer numero);

    @Query("SELECT COALESCE(MAX(v.numero), 0) FROM AppModeleDocumentVersion v WHERE v.modeleId = :modeleId")
    int dernierNumero(@Param("modeleId") UUID modeleId);
}
