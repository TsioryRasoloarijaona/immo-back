package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Passage automatique proposé ; tous les champs sont null quand il n'y a rien à proposer. */
@Schema(description = "Passage automatique de statut proposé après un règlement ou un enregistrement.")
public record TransitionSuggereeDto(
        @Schema(description = "Code du statut proposé, null si aucune proposition.", example = "MINUTE")
        String statut,
        @Schema(description = "Libellé de la transition (StrWorkFlow.Description).", example = "MINUTE")
        String transition,
        @Schema(description = "Date proposée : celle du dernier règlement d'avance, d'auto-financement ou de crédit.")
        LocalDateTime date
) {
    public static final TransitionSuggereeDto AUCUNE = new TransitionSuggereeDto(null, null, null);
}
