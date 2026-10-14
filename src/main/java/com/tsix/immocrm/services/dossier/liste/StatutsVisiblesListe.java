package com.tsix.immocrm.services.dossier.liste;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Statuts de dossier que l'utilisateur courant peut voir dans la liste. Même contrat que
 * {@code services/dossier/workflow/StatutsVisibles.pourUtilisateurCourant()} (lot A), qui le remplacera.
 * Voir FEATURE-vente-liste.md §9.
 */
public interface StatutsVisiblesListe {

    /** Vide = aucune restriction ; sinon {@code ComFolder.StrStatusId} doit être dans l'ensemble (vide = rien). */
    Optional<Set<UUID>> pourUtilisateurCourant();
}
