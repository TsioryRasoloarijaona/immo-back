package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComSaleProspectionView;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Accès en lecture seule à {@link ComSaleProspectionView}.
 *
 * <p>Étend délibérément {@link Repository} et non {@code JpaRepository} : ce dernier exposerait
 * {@code save()}, {@code delete()} et {@code findAll()} sur une vue SQL, dont les deux premiers
 * n'ont pas de sens et dont le troisième est le chemin mesuré à 5 942 ms (voir
 * {@code instructions/FEATURE-vues-legacy-prospection.md} §5). Une seule méthode est donc
 * publiée, et l'interdit est structurel plutôt que documentaire.
 */
public interface ComSaleProspectionViewRepository extends Repository<ComSaleProspectionView, UUID> {

    /**
     * Ligne de vue d'une prospection, ou {@link Optional#empty()} si l'identifiant ne correspond à
     * aucune prospection <em>de vente</em>.
     *
     * <p>Rend systématiquement un {@code Optional} vide pour une <strong>pré-prospection</strong> :
     * la vue exclut les 3 817 lignes portant l'entité {@code ComPreProspection}. L'appelant doit
     * traiter ce cas comme « pas d'enrichissement disponible », jamais comme un 404.
     *
     * <p>Mesuré à 26 ms sur la base réelle (148 colonnes, une ligne).
     */
    Optional<ComSaleProspectionView> findById(UUID id);
}
