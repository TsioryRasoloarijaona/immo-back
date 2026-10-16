package com.tsix.immocrm.dto.modele;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mise en page d'un modèle, stockée en JSON (400 caractères au plus, en-tête et pied compris).")
public record MiseEnPageDto(
        @Schema(description = "Format du papier ; seul `A4` est accepté. Absent = A4.", example = "A4")
        String format,
        @Schema(description = "Marge haute en mm (0 à 60). Absente = 15.", example = "12")
        Integer margeHaut,
        @Schema(description = "Marge basse en mm (0 à 60). Absente = 15.", example = "12")
        Integer margeBas,
        @Schema(description = "Marge gauche en mm (0 à 60). Absente = 15.", example = "16")
        Integer margeGauche,
        @Schema(description = "Marge droite en mm (0 à 60). Absente = 15.", example = "16")
        Integer margeDroite,
        @Schema(description = "En-tête de chaque page, HTML restreint (mêmes règles que le contenu). Absent = aucun.")
        String enTete,
        @Schema(description = "Pied de chaque page, HTML restreint (mêmes règles que le contenu). Absent = aucun.")
        String piedDePage) {
}
