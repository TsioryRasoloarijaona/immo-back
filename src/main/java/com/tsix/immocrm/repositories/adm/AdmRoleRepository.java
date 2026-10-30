package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.dto.administration.RoleLigneDto;
import com.tsix.immocrm.entities.adm.AdmRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmRoleRepository extends JpaRepository<AdmRole, UUID> {

    @Query(value = """
            SELECT new com.tsix.immocrm.dto.administration.RoleLigneDto(r.id, r.code, r.description,
                (SELECT COUNT(u) FROM AdmUserRole u WHERE u.admRole = r),
                (SELECT COUNT(d) FROM AdmRight d WHERE d.admRoleId = r.id))
            FROM AdmRole r
            WHERE :texte = '' OR LOCATE(:texte, LOWER(r.code)) > 0 OR LOCATE(:texte, LOWER(r.description)) > 0""",
            countQuery = """
            SELECT COUNT(r) FROM AdmRole r
            WHERE :texte = '' OR LOCATE(:texte, LOWER(r.code)) > 0 OR LOCATE(:texte, LOWER(r.description)) > 0""")
    Page<RoleLigneDto> chercher(@Param("texte") String texte, Pageable pageable);

    boolean existsByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM AdmRole r WHERE r.id = :id")
    Optional<AdmRole> findPourEcriture(@Param("id") UUID id);

    /** Causes d'un refus de suppression : en base, plusieurs de ces liens partent en cascade avec le rôle. */
    @Query(value = """
            SELECT 'un utilisateur' FROM AdmUserRole WHERE AdmRoleId = :role
            UNION SELECT 'un utilisateur' FROM AdmUser WHERE WebRoleId = :role
            UNION SELECT 'un intervenant' FROM StrIntervener WHERE AdmRoleId = :role
            UNION SELECT 'un rapport' FROM AdmReportDataRole WHERE AdmRoleId = :role
            UNION SELECT 'un workflow' FROM BuyDocumentWorkFlowAdmRole WHERE AdmRoleId = :role
            UNION SELECT 'un workflow' FROM StkFeasibilityStudyWorkFlowAdmRole WHERE AdmRoleId = :role
            UNION SELECT 'un droit web' FROM AdmWebRights WHERE RoleId = :role""", nativeQuery = true)
    List<String> liens(@Param("role") UUID role);
}
