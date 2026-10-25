package com.tsix.immocrm.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Note d'un programme, d'une tranche, d'un article, d'une prospection terrain ou d'une étude (AdmMessage).")
public record NoteDto(
        @Schema(description = "AdmMessage.PKey.") UUID id,
        @Schema(description = "DocDate : date de saisie.") LocalDateTime date,
        @Schema(description = "UserName : nom affiché de l'auteur.") String auteur,
        @Schema(description = "Description : « jj/MM/aaaa HH:mm auteur », écrite à la saisie.") String description,
        @Schema(description = "Notes en texte brut (balises retirées, entités décodées) : à afficher comme texte, jamais "
                + "en HTML.") String texte,
        @Schema(description = "ReminderDate : date de rappel, null si aucune.") LocalDateTime rappel
) {
}
