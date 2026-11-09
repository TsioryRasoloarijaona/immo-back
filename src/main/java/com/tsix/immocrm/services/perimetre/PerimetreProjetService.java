package com.tsix.immocrm.services.perimetre;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Projets de chantier ({@code PrjProject}) visibles de l'appelant, comme OMRAN : ses lignes
 * {@code AdmUserHabilitation.PrjProjectId}, sans rôle exempté ; appelant inconnu ou sans ligne → rien.
 * Partagé par Projet et Achat (règle 6) — voir FEATURE-projet-lots-decomptes.md §2.
 */
@Service
@Transactional(readOnly = true)
public class PerimetreProjetService {

    private final EntityManager entityManager;

    public PerimetreProjetService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Set<UUID> projetsVisibles() {
        return PerimetreCommercialService.admUserIdCourant().map(this::projetsVisiblesDe).orElseGet(Set::of);
    }

    public Set<UUID> projetsVisiblesDe(UUID admUserId) {
        return Set.copyOf(entityManager.createQuery(
                        "select distinct h.prjProjectId from AdmUserHabilitation h"
                                + " where h.admUser.id = :user and h.prjProjectId is not null", UUID.class)
                .setParameter("user", admUserId)
                .getResultList());
    }

    public boolean estVisible(UUID prjProjectId) {
        return prjProjectId != null && projetsVisibles().contains(prjProjectId);
    }

    /** Filtre « projets » demandé ∩ périmètre (OMRAN ne revérifie pas, Arpent si) ; demande vide → tout le périmètre. */
    public Set<UUID> restreindre(Collection<UUID> demandes) {
        Set<UUID> visibles = projetsVisibles();
        if (demandes == null || demandes.isEmpty()) {
            return visibles;
        }
        Set<UUID> retenus = new HashSet<>(demandes);
        retenus.retainAll(visibles);
        return Set.copyOf(retenus);
    }
}
