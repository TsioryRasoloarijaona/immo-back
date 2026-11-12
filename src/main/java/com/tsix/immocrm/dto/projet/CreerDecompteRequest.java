package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Création d'un décompte sur un lot : seule la date est saisie, le reste vient du lot et du décompte précédent.")
public record CreerDecompteRequest(
        @Schema(description = "Date du décompte (DocDate), ≥ date du décompte précédent et ≥ date d'ordre de service du lot.",
                example = "2026-04-01")
        @NotNull LocalDate dateDocument
) {
}
