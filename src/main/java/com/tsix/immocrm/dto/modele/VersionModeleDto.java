package com.tsix.immocrm.dto.modele;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Une version enregistrée d'un modèle (jamais modifiée).")
public record VersionModeleDto(
        @Schema(description = "Numéro de la version, 1, 2, 3…", example = "2")
        int numero,
        @Schema(description = "Date d'enregistrement.")
        LocalDateTime creeLe,
        @Schema(description = "Auteur de l'enregistrement.")
        String creePar,
        @Schema(description = "Vrai pour la version courante du modèle.")
        boolean courante) {
}
