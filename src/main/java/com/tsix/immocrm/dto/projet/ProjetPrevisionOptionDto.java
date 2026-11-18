package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Projet habilité proposé dans la liste de l'écran Prévisions.")
public record ProjetPrevisionOptionDto(
        @Schema(description = "Projet (PrjProject.PKey).") UUID id,
        @Schema(description = "Code du projet.") String code,
        @Schema(description = "Description du projet (libellé de la liste).") String description
) {
}
