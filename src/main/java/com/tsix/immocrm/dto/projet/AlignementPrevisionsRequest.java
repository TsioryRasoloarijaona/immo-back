package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Choix de la modale « Aligner les prévisions au réel ».")
public record AlignementPrevisionsRequest(
        @Schema(description = "INCHANGEE (date fin inchangée), FIXEE (date saisie) ou SYSTEME (date fin proposée).",
                allowableValues = {"INCHANGEE", "FIXEE", "SYSTEME"}) String mode,
        @Schema(description = "Date fin choisie, obligatoire avec FIXEE, ignorée sinon.") LocalDate dateFin
) {
}
