package com.tsix.immocrm.services.dossier.liste;

import com.tsix.immocrm.domain.ChampDateDossier;
import com.tsix.immocrm.domain.LignesListeDossiers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Filtres communs à la liste, aux groupes et à l'export des dossiers : une seule définition pour que les
 * trois portent sur la même population.
 *
 * @param dateVide     {@code true} = date de {@code champDate} vide, {@code false} = renseignée, null = période
 * @param inclureSortis sans lui, annulés et résiliés sont exclus sauf filtre {@code statuts} explicite
 */
public record FiltresListeDossiers(List<String> statuts, UUID commercial, UUID projet, List<UUID> trancheIds,
                                   ChampDateDossier champDate, LocalDateTime dateDebut, LocalDateTime dateFin,
                                   Boolean dateVide, String search, boolean inclureSortis,
                                   LignesListeDossiers lignes) {

    public FiltresListeDossiers {
        lignes = lignes != null ? lignes : LignesListeDossiers.DOSSIERS;
    }

    /** Assiette du tableau de bord par tranche : tous les dossiers de la tranche, sortis compris. */
    public static FiltresListeDossiers deTranche(UUID trancheId) {
        return new FiltresListeDossiers(null, null, null, List.of(trancheId), null, null, null, null, null,
                true, LignesListeDossiers.DOSSIERS);
    }
}
