package com.tsix.immocrm.repositories.str;

import com.tsix.immocrm.entities.str.StrEntity;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@link Repository} et non {@code JpaRepository} : référentiel du légataire, jamais écrit par nous. */
public interface StrEntityRepository extends Repository<StrEntity, UUID> {

    Optional<StrEntity> findById(UUID id);

    /** Types d'une page de prospections, avec les {@code strEntityId} distincts et non nuls. */
    List<StrEntity> findByIdIn(Collection<UUID> ids);

    /** Résout le {@code StrEntityId} « prospection de vente » à la création et à la transformation. */
    List<StrEntity> findByCodeIn(Collection<String> codes);
}
