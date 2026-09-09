package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComSaleProspectionView;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
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

    /**
     * Lignes de vue d'une page de prospections, en une requête.
     *
     * <p>Appelé avec les identifiants de la page déjà chargée depuis {@code ComProspection} : la
     * liste garde ses {@code Specification} — donc ses filtres, son périmètre commercial et sa
     * pagination profonde sur une table indexée — et la vue n'est jamais parcourue.
     *
     * <p>Mesuré sur la base réelle : <strong>7 ms</strong> pour 20 identifiants, 34 ms pour 100.
     * À comparer aux 513 ms qu'aurait coûté le service de la même page depuis la vue à
     * {@code OFFSET 5000}. C'est ce rapport qui a fait retenir l'enrichissement par lot plutôt
     * que le remplacement de la source.
     *
     * <p>Un identifiant sans ligne — pré-prospection, ou prospection sans {@code StrEntityId} —
     * est simplement absent du résultat, et ses colonnes d'activité restent {@code null}.
     */
    List<ComSaleProspectionView> findByIdIn(Collection<UUID> ids);
}
