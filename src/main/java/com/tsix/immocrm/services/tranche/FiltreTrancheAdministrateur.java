package com.tsix.immocrm.services.tranche;

import com.tsix.immocrm.exceptions.ForbiddenException;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Garde du paramètre {@code trancheId} du tableau de bord et de {@code GET /api/clients}
 * ({@code instructions/FEATURE-dashboard-filtre-tranche.md}, R1) : réservé aux administrateurs.
 *
 * <p>Le contrôle lit les autorités de l'appelant authentifié — la même source que
 * {@code @PreAuthorize("hasRole('Administrateur')")} — et jamais un paramètre de requête. Sans
 * {@code trancheId}, il ne fait rien : l'accès d'un commercial reste exactement celui d'avant.
 *
 * <p>403 et non 404 : l'appelant a lui-même nommé un filtre qu'il n'a pas le droit d'utiliser, il
 * n'y a pas d'existence de ressource à cacher.
 */
public final class FiltreTrancheAdministrateur {

    /** Autorité Spring de l'{@code AdmRole} « Administrateur » ({@code hasRole('Administrateur')}). */
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
