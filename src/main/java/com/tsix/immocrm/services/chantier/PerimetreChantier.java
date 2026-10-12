package com.tsix.immocrm.services.chantier;

import java.util.Set;
import java.util.UUID;

/**
 * Projets visibles dans le module Chantier. Contrairement au périmètre commercial, « aucun projet
 * habilité » veut dire « rien à voir », pas « aucune restriction » (comportement OMRAN).
 */
public record PerimetreChantier(boolean toutVoir, Set<UUID> projetIds) {

    public static final PerimetreChantier TOUT = new PerimetreChantier(true, Set.of());
    public static final PerimetreChantier RIEN = new PerimetreChantier(false, Set.of());

    public PerimetreChantier {
        projetIds = Set.copyOf(projetIds);
    }

    public boolean autorise(UUID projetId) {
        return toutVoir || (projetId != null && projetIds.contains(projetId));
    }

    public boolean estVide() {
        return !toutVoir && projetIds.isEmpty();
    }
}
