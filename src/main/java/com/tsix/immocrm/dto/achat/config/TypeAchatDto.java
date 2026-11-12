package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Élément d'une liste de choix de la fiche statut : type d'état (AdmWorkFlowStateType) ou type de document "
        + "à joindre (AdmAttachedFileType).")
public record TypeAchatDto(
        @Schema(description = "PKey de la ligne.") UUID id,
        @Schema(description = "Code.", example = "Intermédiaire") String code,
        @Schema(description = "Libellé affiché.", example = "Intermédiaire") String description
) {
}
