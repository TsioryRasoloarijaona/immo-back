package com.tsix.immocrm.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Création d'une note sur une fiche.")
public record CreerNoteRequest(
        @Schema(description = "Fiche porteuse.", allowableValues = {"projet", "tranche", "article"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String porteur,
        @Schema(description = "PKey du programme, de la tranche ou de l'article.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Contenu HTML ; assaini (liste blanche) avant enregistrement.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "<p>Bornage prévu le 20/10</p>")
        String texteHtml,
        @Schema(description = "Date de rappel facultative (ReminderDate).")
        LocalDateTime rappel
) {
}
