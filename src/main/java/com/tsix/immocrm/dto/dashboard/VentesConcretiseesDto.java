package com.tsix.immocrm.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Assiette légataire : {@code ComFolder.AdvanceDate} dans l'année, statut ≥ CONCRETISATION, hors annulation et désistement. */
public record VentesConcretiseesDto(

        @Schema(description = "Nombre de dossiers dans l'assiette.", example = "4")
        long nombreDossiers,

        @Schema(description = "Somme des prix de vente. `0` si l'assiette est vide.",
                example = "4081900.00")
        BigDecimal prixTotal,

        @Schema(example = "1250000.00")
        BigDecimal montantRegle,

        @Schema(example = "232")
        long dossiersSansDateAvance
) {
}
