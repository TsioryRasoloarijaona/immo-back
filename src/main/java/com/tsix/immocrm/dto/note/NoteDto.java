package com.tsix.immocrm.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Une note OMRAN (AdmMessage) d'un programme, d'une tranche ou d'un article.")
public record NoteDto(
        @Schema(description = "AdmMessage.PKey.")
        UUID id,
        @Schema(description = "Date de saisie (DocDate).")
        LocalDateTime date,
        @Schema(description = "Nom affiché de l'auteur (UserName).", example = "Khadija ELKORRI")
        String auteur,
        @Schema(description = "Libellé OMRAN « jj/MM/aaaa HH:mm Nom ».", example = "12/10/2026 09:30 Khadija ELKORRI")
        String description,
        @Schema(description = "Contenu HTML, assaini côté serveur (balises simples, aucun attribut).")
        String texteHtml,
        @Schema(description = "Date de rappel (ReminderDate), null si aucune.")
        LocalDateTime rappel
) {
}
