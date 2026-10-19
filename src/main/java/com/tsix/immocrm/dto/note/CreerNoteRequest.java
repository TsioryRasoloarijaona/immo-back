package com.tsix.immocrm.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Saisie d'une note en texte simple ; le serveur l'échappe en HTML pour OMRAN.")
public record CreerNoteRequest(
        @Schema(description = "Type du porteur.", allowableValues = {"projet", "tranche", "article"},
                requiredMode = Schema.RequiredMode.REQUIRED) String porteur,
        @Schema(description = "PKey du programme, de la tranche ou de l'article.",
                requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(description = "Texte de la note (sauts de ligne conservés). Vide → 400.",
                requiredMode = Schema.RequiredMode.REQUIRED) String texte,
        @Schema(description = "Date de rappel (ReminderDate), facultative.") LocalDateTime rappel
) {
}
