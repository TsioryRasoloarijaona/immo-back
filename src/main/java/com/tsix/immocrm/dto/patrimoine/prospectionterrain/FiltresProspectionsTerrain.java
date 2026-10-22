package com.tsix.immocrm.dto.patrimoine.prospectionterrain;

import io.swagger.v3.oas.annotations.Parameter;

import java.util.UUID;

/** Filtres de {@code GET /api/prospections-terrain} ; absent = pas de filtre. */
public record FiltresProspectionsTerrain(
        @Parameter(description = "Contient (sans casse) : code, description, ville, catégorie, type, vocation, niveau.",
                example = "TERRAIN") String search,
        @Parameter(description = "Archivées (true), non archivées (false), toutes si absent.") Boolean archivee,
        @Parameter(description = "Transformées en terrain (true) ou non (false).") Boolean transformee,
        @Parameter(description = "Statut (StrStatus).") UUID statutId,
        @Parameter(description = "Vocation foncière (LndVocation).") UUID vocationId,
        @Parameter(description = "Niveau (LndLevel).") UUID niveauId,
        @Parameter(description = "Catégorie (StkItemCategory).") UUID categorieId,
        @Parameter(description = "Ville (AdmCity).") UUID villeId,
        @Parameter(description = "Aménagé (true) ou Brut (false).") Boolean amenage) {
}
