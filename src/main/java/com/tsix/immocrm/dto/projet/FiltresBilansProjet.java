package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.UUID;

/** Filtres de {@code GET /api/projet/bilans} (arbre et export). */
public record FiltresBilansProjet(
        @Parameter(description = "Projets (PrjProject.PKey), répété ; au moins un projet habilité, sinon 400.")
        List<UUID> projets,
        @Parameter(description = "true : montants TTC ; absent ou false : HT.")
        Boolean ttc,
        @Parameter(description = "Code de la colonne 1 de l'écart (un des 8 codes de `colonnes`).",
                example = "marche")
        String colonne1,
        @Parameter(description = "Code de la colonne 2 de l'écart, soustraite de la colonne 1.",
                example = "estimationBase")
        String colonne2) {

    public FiltresBilansProjet {
        projets = projets == null ? List.of() : List.copyOf(projets);
        ttc = Boolean.TRUE.equals(ttc);
    }
}
