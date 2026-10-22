package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmReportData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmReportDataRepository extends JpaRepository<AdmReportData, UUID> {

    List<AdmReportData> findByTypeIn(Collection<String> types);

    Optional<AdmReportData> findFirstByTypeInAndCode(Collection<String> types, String code);

    /** Rôles du compte avec {@code WithReportRestriction = 0} : au moins un ⇒ compte libre (NULL compte restreint). */
    @Query("SELECT COUNT(ur) FROM AdmUserRole ur WHERE ur.admUser.id = :admUserId "
            + "AND ur.admRole.withReportRestriction = false")
    long compterRolesLibres(UUID admUserId);

    /** Rapports liés à l'un des rôles du compte (AdmUserRole relu à chaque appel). */
    @Query("SELECT DISTINCT d.admReportData.id FROM AdmReportDataRole d, AdmUserRole ur "
            + "WHERE ur.admRole = d.admRole AND ur.admUser.id = :admUserId")
    List<UUID> rapportsLies(UUID admUserId);
}
