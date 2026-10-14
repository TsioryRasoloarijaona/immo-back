package com.tsix.immocrm.repositories.str;

import com.tsix.immocrm.entities.str.StrWorkFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface StrWorkFlowRepository extends JpaRepository<StrWorkFlow, UUID> {

    boolean existsByStrConfigEntityId(UUID configEntityId);

    /** Comme {@code StrStatus.GetNextWorkFlowList} : départ et arrivée renseignés, auto-transition exclue. */
    @Query("SELECT w FROM StrWorkFlow w WHERE w.strConfigEntityId = :entite AND w.statusFromId = :depuis "
            + "AND w.statusToId IS NOT NULL AND w.statusToId <> :depuis ORDER BY w.sorting ASC, w.code ASC")
    List<StrWorkFlow> sortantes(UUID entite, UUID depuis);
}
