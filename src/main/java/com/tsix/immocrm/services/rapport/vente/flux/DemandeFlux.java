package com.tsix.immocrm.services.rapport.vente.flux;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Entrée de {@link FluxVenteMensuel#calculer} : {@code tranches} = exactement {@code ContexteRapport.tranchesVisibles()}
 * (éventuellement réduit) ; bornes d'années inclusives, {@code null} = sans borne.
 */
public record DemandeFlux(List<UUID> tranches, Integer anneeDebut, Integer anneeFin, Set<MesureFlux> mesures) {

    public DemandeFlux {
        if (tranches == null) {
            throw new IllegalArgumentException("Liste de tranches absente : vide = aucune tranche, jamais « toutes ».");
        }
        if (mesures == null || mesures.isEmpty()) {
            throw new IllegalArgumentException("Aucune mesure demandée.");
        }
        if (anneeDebut != null && anneeFin != null && anneeDebut > anneeFin) {
            throw new IllegalArgumentException("Année de début %d après l'année de fin %d.".formatted(anneeDebut, anneeFin));
        }
        tranches = List.copyOf(tranches);
        mesures = Set.copyOf(mesures);
    }

    boolean retient(int annee) {
        return (anneeDebut == null || annee >= anneeDebut) && (anneeFin == null || annee <= anneeFin);
    }
}
