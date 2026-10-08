package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComSaleProspectionView;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link Repository} et non {@code JpaRepository} : pas de {@code save}/{@code delete} sur une vue, et
 * pas de {@code findAll}, le chemin lent de FEATURE-vues-legacy-prospection.md §5.
 */
public interface ComSaleProspectionViewRepository extends Repository<ComSaleProspectionView, UUID> {

    /** Vide pour une pré-prospection (exclue de la vue) : « pas d'enrichissement », jamais un 404. */
    Optional<ComSaleProspectionView> findById(UUID id);

    /**
     * Enrichit une page déjà chargée depuis {@code ComProspection} (filtres, périmètre, pagination y
     * restent) : la vue n'est jamais parcourue. Sans ligne (pré-prospection, sans {@code StrEntityId}) = absent.
     */
    List<ComSaleProspectionView> findByIdIn(Collection<UUID> ids);
}
