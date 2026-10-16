package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Passer à POINTED exige une pièce jointe (409 sinon), {@code dateReglement} et {@code compteId} (400 sinon) :
 * voir FEATURE-reglement-statut-preuve.md §2 et FEATURE-dossier-durcissement.md §4.
 */
@Schema(description = "Changement de statut d'un règlement.")
public record ModifierStatutReglementRequest(

        @Schema(example = "POINTED",
                allowableValues = {"POINTED", "CANCELED"})
        String statut,

        @Schema(example = "2026-09-01T10:00:00")
        LocalDateTime dateReglement,

        @Schema(example = "acfd8693-0175-42a0-9e0a-37f6dcc7cfdc")
        UUID compteId
) {
}
