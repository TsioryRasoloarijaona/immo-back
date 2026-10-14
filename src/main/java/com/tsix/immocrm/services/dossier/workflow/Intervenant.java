package com.tsix.immocrm.services.dossier.workflow;

import com.tsix.immocrm.entities.str.StrIntervener;

import java.util.Set;
import java.util.UUID;

/**
 * Qui agit sur le workflow : son compte ({@code AdmUser}, nul pour un principal sans compte) et ses rôles
 * ({@code AdmRole.PKey}). L'Administrateur passe outre les intervenants (décision utilisateur, FEATURE-vente-workflow.md).
 */
public record Intervenant(UUID admUserId, Set<UUID> roleIds, boolean administrateur) {

    public Intervenant {
        roleIds = Set.copyOf(roleIds);
    }

    /** Comme {@code StrStatus.GetNextWorkFlowList} : le rôle de l'intervenant, ou l'utilisateur lui-même. */
    public boolean correspond(StrIntervener intervenant) {
        return (intervenant.getAdmRoleId() != null && roleIds.contains(intervenant.getAdmRoleId()))
                || (intervenant.getAdmUserId() != null && intervenant.getAdmUserId().equals(admUserId));
    }
}
