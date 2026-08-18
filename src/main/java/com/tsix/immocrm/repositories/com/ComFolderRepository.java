package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ComFolderRepository extends JpaRepository<ComFolder, UUID>, JpaSpecificationExecutor<ComFolder> {

    /**
     * Lightweight projection for {@code GET /api/dashboard} — avoids loading full ComFolder
     * rows (100+ columns) for an aggregate. Tuple: [id, statutCode, totalToPay, revival1,
     * revival1ValidityDate, revival2, revival2ValidityDate, customerSocialReason,
     * customerFirstName, customerLastName].
     */
    @Query("SELECT f.id, s.code, f.totalToPay, f.revival1, f.revival1ValidityDate, f.revival2, f.revival2ValidityDate, " +
            "c.socialReason, c.firstName, c.lastName " +
            "FROM ComFolder f LEFT JOIN f.strStatus s LEFT JOIN f.cfgCustomer c")
    List<Object[]> findDashboardProjection();
}
