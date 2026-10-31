package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.Parameter;

import java.util.UUID;

public record FiltresUtilisateurs(
        @Parameter(description = "Sous-chaîne sur code, description ou email, casse ignorée.")
        String search,

        @Parameter(description = "Filtre sur le statut désactivé. Absent = tous les comptes.")
        Boolean disabled,

        @Parameter(description = "Comptes portant ce rôle (AdmUserRole).")
        UUID roleId,

        @Parameter(description = "Comptes liés à ce personnel (CfgEmployeeId).")
        UUID personnelId
) {
}
