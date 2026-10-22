package com.tsix.immocrm.repositories.lnd;

import com.tsix.immocrm.entities.lnd.LndLand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LndLandRepository extends JpaRepository<LndLand, UUID>, JpaSpecificationExecutor<LndLand> {

    /** Code déjà pris par une autre prospection (collation sans casse). */
    boolean existsByCodeAndIdNot(String code, UUID id);

    /** Études de faisabilité par prospection ; tuple [LndLandId, nombre]. */
    @Query("SELECT e.lndLandId, COUNT(e) FROM StkFeasibilityStudy e WHERE e.lndLandId IN :ids GROUP BY e.lndLandId")
    List<Object[]> etudesParProspection(Collection<UUID> ids);

    /** Codes des types de statut ({@code StrStatusType}, sans entité JPA) ; tuple [PKey, Code]. */
    @Query(value = "SELECT PKey, Code FROM StrStatusType", nativeQuery = true)
    List<Object[]> typesDeStatut();
}
