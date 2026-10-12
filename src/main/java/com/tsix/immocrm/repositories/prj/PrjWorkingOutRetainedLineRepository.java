package com.tsix.immocrm.repositories.prj;

import com.tsix.immocrm.entities.prj.PrjWorkingOutRetainedLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PrjWorkingOutRetainedLineRepository extends JpaRepository<PrjWorkingOutRetainedLine, UUID> {

    @Query("SELECT r FROM PrjWorkingOutRetainedLine r WHERE r.prjWorkingOut.id = :decompteId ORDER BY r.sorting ASC")
    List<PrjWorkingOutRetainedLine> findByDecompte(@Param("decompteId") UUID decompteId);
}
