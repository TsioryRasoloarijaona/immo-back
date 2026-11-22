package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.UUID;

/** Filtres de {@code GET /api/projet/tresorerie} (arbre, cellule, export). */
public record FiltresTresorerieProjet(
        @Parameter(description = "Année civile ; absente = année du jour.", example = "2026")
        Integer annee,
        @Parameter(description = "Projets (PrjProject.PKey), répété ; au moins un projet habilité, sinon 400.")
        List<UUID> projets,
        @Parameter(description = "Société (CfgCompany.PKey) ; absente = toutes les sociétés habilitées.")
        UUID societe) {

    public FiltresTresorerieProjet {
        projets = projets == null ? List.of() : List.copyOf(projets);
    }
}
