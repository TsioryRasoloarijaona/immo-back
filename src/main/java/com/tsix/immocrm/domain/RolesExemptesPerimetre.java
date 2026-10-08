package com.tsix.immocrm.domain;

import java.util.Set;

/**
 * Codes {@code AdmRole.Code} exemptés du périmètre commercial, comparés au code exact (jamais par
 * préfixe) ; rôles lus dans {@code AdmUserRole}, pas {@code AdmUser.WebRoleId} (ils divergent).
 * {@code Commercial Chef} n'est pas exempté — voir FEATURE-perimetre-commercial.md §6.2.
 */
public final class RolesExemptesPerimetre {

    public static final Set<String> CODES = Set.of("Administrateur", "Direction Générale");

    private RolesExemptesPerimetre() {
    }

    public static boolean exempte(Iterable<String> codesRole) {
        for (String code : codesRole) {
            if (code != null && CODES.contains(code)) {
                return true;
            }
        }
        return false;
    }
}
