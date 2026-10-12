package com.tsix.immocrm.services.equipe;

import java.util.Set;
import java.util.UUID;

/**
 * Compte actif portant le rôle Commercial ou Commercial Aide. {@code projetIds} = toutes ses affectations
 * projet ; vide = sans restriction (il voit tous les projets, FEATURE-perimetre-commercial.md §6.3).
 */
public record MembreEquipe(UUID admUserId, String code, String nom, UUID employeId,
                           Set<String> roles, Set<UUID> projetIds) {

    public boolean sansRestriction() {
        return projetIds.isEmpty();
    }

    /** Seule règle « affecté au projet de la ligne » : distribuer et réaffecter passent par elle. */
    public boolean peutRecevoir(UUID projetId) {
        return sansRestriction() || (projetId != null && projetIds.contains(projetId));
    }

    public boolean estProtege() {
        return RolesEquipe.estProtege(roles);
    }
}
