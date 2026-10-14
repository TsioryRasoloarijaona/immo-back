package com.tsix.immocrm.services.dossier.workflow;

import com.tsix.immocrm.repositories.str.StrIntervenerRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * L'appelant comme intervenant : rôles de {@code AdmUserRole} (ceux qu'OMRAN lit) et rôles du jeton
 * ({@code ROLE_<code>}, seuls connus d'un principal sans compte).
 */
@Component
public class IntervenantCourant {

    static final String ROLE_ADMINISTRATEUR = "Administrateur";
    static final String DROIT_SEUIL = "Yes.Com.Front.Sales.Monitoring.toolVerifyThreshold";
    private static final String PREFIXE_ROLE = "ROLE_";

    private final StrIntervenerRepository intervenerRepository;

    public IntervenantCourant(StrIntervenerRepository intervenerRepository) {
        this.intervenerRepository = intervenerRepository;
    }

    public Intervenant courant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return new Intervenant(null, Set.of(), false);
        }
        UUID admUserId = auth.getPrincipal() instanceof AuthenticatedUser user ? user.admUserId() : null;
        Set<String> codes = new HashSet<>();
        for (GrantedAuthority autorite : auth.getAuthorities()) {
            String nom = autorite.getAuthority();
            if (nom != null && nom.startsWith(PREFIXE_ROLE)) {
                codes.add(nom.substring(PREFIXE_ROLE.length()));
            }
        }
        Set<UUID> roleIds = new HashSet<>();
        if (admUserId != null) {
            for (Object[] role : intervenerRepository.rolesDe(admUserId)) {
                roleIds.add((UUID) role[0]);
                if (role[1] != null) {
                    codes.add((String) role[1]);
                }
            }
        }
        if (!codes.isEmpty()) {
            roleIds.addAll(intervenerRepository.rolesParCode(codes));
        }
        return new Intervenant(admUserId, roleIds, codes.contains(ROLE_ADMINISTRATEUR));
    }

    /**
     * {@code Sale.cs:VerifThreshold} ne contrôle le seuil que pour un utilisateur dont un rôle porte le droit
     * {@link #DROIT_SEUIL} ; droit absent de {@code AdmRight} (base de test) : tout le monde est contrôlé.
     */
    public boolean verifieLeSeuil(Intervenant intervenant) {
        List<UUID> roles = intervenerRepository.rolesAvecDroit(DROIT_SEUIL);
        return roles.isEmpty() || roles.stream().anyMatch(intervenant.roleIds()::contains);
    }
}
