package com.tsix.immocrm.services.equipe;

import com.tsix.immocrm.domain.RolesExemptesPerimetre;

import java.util.Collection;
import java.util.Set;

/** Codes {@code AdmRole.Code} de l'écran « Mon équipe », comparés au code exact (FEATURE-equipe-commerciale.md §1). */
public final class RolesEquipe {

    public static final String CHEF = "Commercial Chef";
    public static final String COMMERCIAL = "Commercial";
    public static final String COMMERCIAL_AIDE = "Commercial Aide";

    /** Rôles qui font d'un compte un membre possible d'une équipe. */
    static final Set<String> MEMBRES = Set.of(COMMERCIAL, COMMERCIAL_AIDE);

    private RolesEquipe() {
    }

    static boolean estMembrePossible(Collection<String> roles) {
        return roles.stream().anyMatch(MEMBRES::contains);
    }

    /** Un chef ne modifie ni un autre chef (ni lui-même), ni un administrateur, ni la direction générale. */
    static boolean estProtege(Collection<String> roles) {
        return roles.contains(CHEF) || RolesExemptesPerimetre.exempte(roles);
    }
}
