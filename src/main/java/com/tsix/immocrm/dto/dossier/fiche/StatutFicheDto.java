package com.tsix.immocrm.dto.dossier.fiche;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Statut du dossier et sa date de validité (lecture seule).")
public record StatutFicheDto(
        @Schema(description = "Code du statut (StrStatus.Code), null si le dossier n'a pas de statut.")
        String code,
        @Schema(description = "Date de validité du statut (ComFolder.StatusValidityDate), null si sans limite.")
        LocalDateTime dateValidite,
        @Schema(description = "Vrai si la date de validité est passée (comparée à l'heure de Casablanca).")
        boolean expire
) {
}
