package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmEMailHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AdmEMailHistoryRepository
        extends JpaRepository<AdmEMailHistory, UUID>, JpaSpecificationExecutor<AdmEMailHistory> {

    @Query("SELECT DISTINCT e.admUserDescription FROM AdmEMailHistory e "
            + "WHERE e.admUserDescription <> '' ORDER BY e.admUserDescription ASC")
    List<String> utilisateurs();
}
