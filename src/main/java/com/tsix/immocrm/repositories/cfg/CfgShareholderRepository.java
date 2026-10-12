package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgShareholder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfgShareholderRepository extends JpaRepository<CfgShareholder, UUID> {

    /** {@code motif} = {@code %texte%} sans espaces, ou {@code null} pour tout ; espaces retirés en base aussi (OMRAN). */
    @Query("SELECT s FROM CfgShareholder s WHERE :motif IS NULL "
            + "OR REPLACE(s.code, ' ', '') LIKE :motif OR REPLACE(s.description, ' ', '') LIKE :motif "
            + "ORDER BY s.code ASC")
    List<CfgShareholder> rechercher(@Param("motif") String motif);

    /** Unicité du code sur toute la table (collation French_CI_AS). */
    Optional<CfgShareholder> findFirstByCodeAndIdNot(String code, UUID id);
}
