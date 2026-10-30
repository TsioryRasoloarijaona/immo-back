package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.dto.administration.LigneHistoriqueDto;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AdmWebHistoricsRepository extends JpaRepository<AdmWebHistorics, UUID> {

    String FILTRES = """
            FROM AdmWebHistorics h
            WHERE h.actionDateTime >= :debut AND h.actionDateTime < :finExclue
            AND (:texte IS NULL OR h.objectType LIKE :texte OR h.objectLabel LIKE :texte)
            AND (:utilisateur IS NULL OR h.userDescription = :utilisateur)
            AND (:action IS NULL OR h.action = :action)""";

    /** Lignes de l'objet et de ses enfants (comme OMRAN), limitées aux types donnés pour l'objet comme pour le parent. */
    @Query("SELECT h FROM AdmWebHistorics h WHERE (h.objectId = :objetId OR h.parentObjectId = :objetId) "
            + "AND h.objectType IN :types AND (h.parentObjectType IS NULL OR h.parentObjectType IN :types)")
    Page<AdmWebHistorics> journal(UUID objetId, Collection<String> types, Pageable pageable);

    /** Projection : ne charge jamais {@code Details}, colonne {@code text} volumineuse. */
    @Query(value = "SELECT new com.tsix.immocrm.dto.administration.LigneHistoriqueDto(h.id, h.objectId, h.actionDateTime, "
            + "h.userDescription, h.objectType, h.objectLabel, h.action) " + FILTRES,
            countQuery = "SELECT COUNT(h) " + FILTRES)
    Page<LigneHistoriqueDto> lignes(LocalDateTime debut, LocalDateTime finExclue, String texte, String utilisateur,
                                    String action, Pageable pageable);

    @Query("SELECT DISTINCT h.userDescription FROM AdmWebHistorics h "
            + "WHERE h.actionDateTime >= :debut AND h.actionDateTime < :finExclue "
            + "AND h.userDescription IS NOT NULL AND h.userDescription <> '' ORDER BY h.userDescription")
    List<String> utilisateurs(LocalDateTime debut, LocalDateTime finExclue);
}
