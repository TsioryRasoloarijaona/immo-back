package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AdmUserRoleRepository extends JpaRepository<AdmUserRole, UUID> {

    /** Alimente le claim {@code roles} du JWT. */
    List<AdmUserRole> findByAdmUserId(UUID admUserId);

    List<AdmUserRole> findByAdmUserIdIn(List<UUID> admUserIds);

    /** Toutes les attributions, compte et rôle chargés : table de quelques dizaines de lignes (équipe commerciale). */
    @Query("SELECT ur FROM AdmUserRole ur JOIN FETCH ur.admUser JOIN FETCH ur.admRole")
    List<AdmUserRole> findAllAvecCompteEtRole();
}
