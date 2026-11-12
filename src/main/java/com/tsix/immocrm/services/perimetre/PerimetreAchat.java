package com.tsix.immocrm.services.perimetre;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Pièces d'achat visibles, comme les listes OMRAN : société habilitée ET (projet retenu OU, sans filtre projet, pièce sans
 * projet) ; {@code moduleProjet} ajoute le module PRJ des écrans Dépenses (FEATURE-visibilite-omran.md R3, R4, R5).
 */
public record PerimetreAchat(Set<UUID> societes, Set<UUID> projets, boolean sansProjet, boolean moduleProjet) {

    public PerimetreAchat {
        societes = Set.copyOf(societes);
        projets = Set.copyOf(projets);
    }

    public boolean vide() {
        return societes.isEmpty() || (projets.isEmpty() && !sansProjet);
    }

    public PerimetreAchat duModuleProjet() {
        return new PerimetreAchat(societes, projets, sansProjet, true);
    }

    /** Sociétés habilitées ∩ {@code demandees} (vide = toutes) : une société hors périmètre est ignorée, jamais ajoutée. */
    public PerimetreAchat deSocietes(Collection<UUID> demandees) {
        if (demandees.isEmpty()) {
            return this;
        }
        Set<UUID> retenues = new HashSet<>(demandees);
        retenues.retainAll(societes);
        return new PerimetreAchat(retenues, projets, sansProjet, moduleProjet);
    }
}
