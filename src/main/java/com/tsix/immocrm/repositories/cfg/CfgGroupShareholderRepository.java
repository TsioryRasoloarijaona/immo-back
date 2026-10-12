package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfgGroupShareholderRepository extends JpaRepository<CfgGroupShareholder, UUID> {

    /** Même recherche que {@code CfgGroupShareholder.GetList} : code et description, espaces retirés. */
    @Query("SELECT g FROM CfgGroupShareholder g WHERE :motif IS NULL "
            + "OR REPLACE(g.code, ' ', '') LIKE :motif OR REPLACE(g.description, ' ', '') LIKE :motif "
            + "ORDER BY g.code ASC")
    List<CfgGroupShareholder> rechercher(@Param("motif") String motif);

    Optional<CfgGroupShareholder> findFirstByCodeAndIdNot(String code, UUID id);
}
