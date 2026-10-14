package com.tsix.immocrm.services.dossier.liste;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Implémentation d'attente : tous les statuts, jusqu'au branchement de {@code StatutsVisibles} (lot A). */
@Component
public class TousLesStatutsVisibles implements StatutsVisiblesListe {

    @Override
    public Optional<Set<UUID>> pourUtilisateurCourant() {
        return Optional.empty();
    }
}
