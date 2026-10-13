package com.tsix.immocrm.dto.dossier.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Instruction adressée au commercial du dossier (action de type « Instruction »).")
public record CreerInstructionRequest(
        @Schema(description = "Texte de l'instruction. Requis, non vide.")
        String note,
        @Schema(description = "Date d'alerte éventuelle ; absente = pas d'alerte.")
        LocalDateTime dateAlerte
) {
}
