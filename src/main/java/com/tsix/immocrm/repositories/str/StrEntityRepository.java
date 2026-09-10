package com.tsix.immocrm.repositories.str;

import com.tsix.immocrm.entities.str.StrEntity;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès en lecture seule au référentiel {@link StrEntity}.
 *
 * <p>Étend {@link Repository} et non {@code JpaRepository}, pour la même raison que
 * {@code ComSaleProspectionViewRepository} : ce dernier exposerait {@code save()} et
 * {@code delete()} sur un référentiel de configuration du légataire, que nous n'écrivons jamais.
 * L'interdit est structurel plutôt que documentaire.
 *
 * <p>{@code findAll()} est en revanche publié ici, contrairement au dépôt de vue : la table ne
 * compte que 162 lignes et son parcours complet est sans danger.
 */
public interface StrEntityRepository extends Repository<StrEntity, UUID> {

    /** Un type de document par identifiant, vide si l'identifiant est inconnu. */
    Optional<StrEntity> findById(UUID id);

    /**
     * Résout d'un coup les types d'une page de prospections.
     *
     * <p>Appelé avec les {@code strEntityId} <em>distincts</em> de la page — en pratique un ou
     * deux, la quasi-totalité des lignes portant {@code ComProspection}. Le coût est celui d'une
     * requête par clé sur une table de 162 lignes.
     *
     * <p>Un identifiant {@code NULL} côté prospection n'a pas à être passé : il n'a pas de type,
     * et ses trois champs restent {@code null}.
     */
    List<StrEntity> findByIdIn(Collection<UUID> ids);

    /** Résolution par code métier — sert le filtre {@code ?typeProspection=}. */
    List<StrEntity> findByCodeIn(Collection<String> codes);
}
