package com.tsix.immocrm.dto.dossier.fiche;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Onglet « Annulation / Résiliation » ; vide tant que le dossier n'est pas sorti.")
public record AnnulationFicheDto(
        @Schema(description = "Date d'annulation / résiliation (ComFolder.CancelDate).")
        LocalDateTime date,
        @Schema(description = "Causes saisies (ComFolder.CancelDescription).")
        String causes
) {
}
