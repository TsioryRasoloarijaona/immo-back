package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat de « Mettre à jour les schémas de financement ».")
public record MiseAJourSchemasDto(
        @Schema(description = "Échéances dont la date a été déplacée.") int documentsModifies,
        @Schema(description = "Dossiers distincts de ces échéances.") int dossiersConcernes
) {
}
