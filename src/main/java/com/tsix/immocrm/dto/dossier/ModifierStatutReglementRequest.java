package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Passer à POINTED exige une pièce jointe (409 sinon) et {@code dateReglement} (400 sinon) : statut et date
 * d'encaissement n'ont jamais divergé en base. Voir FEATURE-reglement-statut-preuve.md §2.
 */
@Schema(description = "Changement de statut d'un règlement.")
public record ModifierStatutReglementRequest(

        @Schema(example = "POINTED",
                allowableValues = {"POINTED", "CANCELED"})
        String statut,

        @Schema(example = "2026-09-01T10:00:00")
        LocalDateTime dateReglement
) {
}
