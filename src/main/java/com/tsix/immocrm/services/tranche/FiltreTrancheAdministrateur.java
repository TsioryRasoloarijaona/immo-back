package com.tsix.immocrm.services.tranche;

import com.tsix.immocrm.exceptions.ForbiddenException;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * {@code trancheId} du tableau de bord et des clients réservé aux administrateurs, lu dans les autorités
 * (comme {@code hasRole}), jamais dans la requête ; 403, rien à cacher ({@code FEATURE-dashboard-filtre-tranche.md} R1).
 */
public final class FiltreTrancheAdministrateur {

    static final String ROLE_ADMINISTRATEUR = "ROLE_Administrateur";

    private FiltreTrancheAdministrateur() {
    }

    public static void verifier(@Nullable UUID trancheId) {
        if (trancheId == null) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean administrateur = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMINISTRATEUR.equals(a.getAuthority()));
        if (!administrateur) {
            throw new ForbiddenException("Le filtre trancheId est réservé aux administrateurs.");
        }
    }
}
