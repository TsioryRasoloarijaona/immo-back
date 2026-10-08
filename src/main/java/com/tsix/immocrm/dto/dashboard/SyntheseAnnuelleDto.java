package com.tsix.immocrm.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Les requêtes de l'écran d'accueil d'OMRAN, relevées dans sa trace d'audit — voir FEATURE-dashboard-omran.md. */
public record SyntheseAnnuelleDto(

        @Schema(description = "Année observée.", example = "2026")
        int annee,

        List<PointMensuelDto> echeancier,

        @Schema(description = "Les ventes concrétisées de l'année.")
        VentesConcretiseesDto ventesConcretisees,

        @Schema(description = "Les encaissements de l'année.")
        EncaissementsAnnuelsDto encaissements
) {
}
