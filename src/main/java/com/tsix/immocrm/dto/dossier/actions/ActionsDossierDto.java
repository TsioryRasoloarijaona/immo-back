package com.tsix.immocrm.dto.dossier.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Onglet « Actions » du dossier : la liste des actions et leur compteur par type.")
public record ActionsDossierDto(
        @Schema(description = "Actions du dossier, de la plus récente à la plus ancienne.")
        List<ActionDossierDto> actions,
        @Schema(description = "Compteur par type d'action, trié par nombre décroissant.")
        List<CompteurTypeActionDto> parType
) {
}
