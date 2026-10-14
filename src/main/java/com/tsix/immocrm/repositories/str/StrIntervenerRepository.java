package com.tsix.immocrm.repositories.str;

import com.tsix.immocrm.entities.str.StrIntervener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StrIntervenerRepository extends JpaRepository<StrIntervener, UUID> {

    /** Intervenants des transitions données ; tuple [StrWorkFlowId, intervenant]. Second valideur exclu. */
    @Query("SELECT wi.strWorkFlowId, i FROM StrWorkFlowIntervener wi, StrIntervener i "
            + "WHERE i.id = wi.strIntervenerId AND wi.strWorkFlowId IN :transitions "
            + "AND (wi.withPassword IS NULL OR wi.withPassword = false)")
    List<Object[]> deTransitions(Collection<UUID> transitions);

    /** Intervenants des statuts d'une entité ; tuple [StrStatusId, intervenant]. */
    @Query("SELECT si.strStatusId, i FROM StrStatusIntervener si, StrIntervener i, StrStatus s "
            + "WHERE i.id = si.strIntervenerId AND s.id = si.strStatusId AND s.strConfigEntityId = :entite")
    List<Object[]> deStatuts(UUID entite);

    /** {@code AdmRole.PKey} des codes de rôle donnés (rôles portés par le jeton). */
    @Query("SELECT r.id FROM AdmRole r WHERE r.code IN :codes")
    List<UUID> rolesParCode(Collection<String> codes);

    /** Rôles qui portent le droit {@code code} ({@code AdmRight}) ; liste vide = droit non paramétré. */
    @Query("SELECT r.admRoleId FROM AdmRight r WHERE r.code = :code")
    List<UUID> rolesAvecDroit(String code);

    /** {@code AdmRole} de l'utilisateur ; tuple [PKey, Code]. */
    @Query("SELECT ur.admRole.id, ur.admRole.code FROM AdmUserRole ur WHERE ur.admUser.id = :admUserId")
    List<Object[]> rolesDe(UUID admUserId);
}
