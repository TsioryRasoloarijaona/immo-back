package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkFeasibilityStudy;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Études de faisabilité sous la règle de visibilité par société ({@code VisibiliteEntites.CLAUSE_JPQL}). */
public interface StkFeasibilityStudyRepository extends JpaRepository<StkFeasibilityStudy, UUID> {

    String VISIBLES = "SELECT e FROM StkFeasibilityStudy e LEFT JOIN e.cfgCompany soc WHERE " + VisibiliteEntites.CLAUSE_JPQL;

    @Query(VISIBLES + " AND e.id = :id")
    Optional<StkFeasibilityStudy> findVisible(@Param("id") UUID id);

    /** Inconnue ou d'une société inactive → 404 (FEATURE-patrimoine-faisabilite.md §8 ter.7). */
    default StkFeasibilityStudy exigerVisible(UUID id) {
        return findVisible(id).orElseThrow(() -> new ResourceNotFoundException("Étude de faisabilité introuvable: " + id));
    }

    @Query(VISIBLES + " AND (:societeId IS NULL OR soc.id = :societeId)"
            + " AND (:statutId IS NULL OR e.status.id = :statutId)"
            + " AND (:motif IS NULL OR LOWER(e.code) LIKE :motif OR LOWER(e.description) LIKE :motif) ORDER BY e.code")
    List<StkFeasibilityStudy> lister(@Param("societeId") UUID societeId, @Param("statutId") UUID statutId,
                                     @Param("motif") String motif);

    Optional<StkFeasibilityStudy> findFirstByCode(String code);
}
