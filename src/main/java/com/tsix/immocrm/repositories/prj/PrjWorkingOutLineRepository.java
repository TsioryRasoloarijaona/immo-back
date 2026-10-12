package com.tsix.immocrm.repositories.prj;

import com.tsix.immocrm.entities.prj.PrjWorkingOutLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PrjWorkingOutLineRepository extends JpaRepository<PrjWorkingOutLine, UUID> {

    @Query("SELECT l FROM PrjWorkingOutLine l LEFT JOIN FETCH l.prjMarketLine "
            + "WHERE l.prjWorkingOut.id = :decompteId ORDER BY l.sorting ASC, l.code ASC")
    List<PrjWorkingOutLine> findByDecompte(@Param("decompteId") UUID decompteId);
}
