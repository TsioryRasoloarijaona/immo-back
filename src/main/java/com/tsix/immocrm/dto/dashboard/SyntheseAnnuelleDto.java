package com.tsix.immocrm.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * {@code GET /api/dashboard/annuel} — l'écran d'accueil du logiciel de bureau, porté tel quel.
 *
 * <p>Les trois blocs reprennent les sept requêtes qu'OMRAN exécute à l'ouverture de son tableau de
 * bord, relevées dans sa trace d'audit SQL. Voir
 * {@code instructions/FEATURE-dashboard-omran.md}.
 */
@Schema(description = "Synthèse annuelle : l'échéancier mois par mois, les ventes concrétisées et "
        + "les encaissements de l'année. Reprend l'écran d'accueil du logiciel de bureau, y "
        + "compris son assiette, pour que les chiffres restent comparables pendant la transition.")
public record SyntheseAnnuelleDto(

        @Schema(description = "Année observée.", example = "2026")
        int annee,

        @Schema(description = "**Toujours 12 points, de janvier à décembre.** Un mois sans "
                + "échéance vaut zéro et n'est jamais omis : une courbe doit pouvoir se tracer "
                + "sans reboucher de trous côté client.")
        List<PointMensuelDto> echeancier,

        @Schema(description = "Les ventes concrétisées de l'année.")
        VentesConcretiseesDto ventesConcretisees,

        @Schema(description = "Les encaissements de l'année.")
        EncaissementsAnnuelsDto encaissements
) {
}
