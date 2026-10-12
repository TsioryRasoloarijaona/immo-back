package com.tsix.immocrm.repositories.prj;

import com.tsix.immocrm.entities.prj.PrjWorkingOutLineExtra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PrjWorkingOutLineExtraRepository extends JpaRepository<PrjWorkingOutLineExtra, UUID> {

    @Query("SELECT x FROM PrjWorkingOutLineExtra x WHERE x.prjWorkingOut.id = :decompteId "
            + "ORDER BY x.sorting ASC, x.code ASC")
    List<PrjWorkingOutLineExtra> findByDecompte(@Param("decompteId") UUID decompteId);
}
