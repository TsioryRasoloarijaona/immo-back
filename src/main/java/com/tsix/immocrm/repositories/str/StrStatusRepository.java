package com.tsix.immocrm.repositories.str;

import com.tsix.immocrm.entities.str.StrStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface StrStatusRepository extends JpaRepository<StrStatus, UUID> {

    /**
     * Statuts des entités de lot (« PrjMarket* »), par jointure sur l'entité de configuration : aucune PKey
     * en dur. SQL natif car {@code StrStatusTypeId} n'est pas mappé. Tuple : [id, code, description,
     * ordre, typeCode, entiteCode].
     */
    @Query(value = "SELECT s.PKey, s.Code, s.Description, s.Sorting, t.Code AS TypeCode, e.Code AS EntiteCode "
            + "FROM StrStatus s JOIN StrEntity e ON e.PKey = s.StrConfigEntityId "
            + "LEFT JOIN StrStatusType t ON t.PKey = s.StrStatusTypeId "
            + "WHERE e.Code LIKE 'PrjMarket%' ORDER BY e.Code ASC, s.Sorting ASC, s.Code ASC", nativeQuery = true)
    List<Object[]> findStatutsDesLots();

    List<StrStatus> findByStrConfigEntityId(UUID configEntityId);

    StrStatus findByStrConfigEntityIdAndCode(UUID configEntityId, String code);
}
