package com.tsix.immocrm.dto.modele;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Mise en page d'un modèle, en JSON ; texte de l'en-tête et du pied compté avec le contenu (1 Mo).")
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
        String piedDePage,
        @Schema(description = "Image de fond de chaque page (id d'une image déposée, PDF seulement). Absente = aucun fond.")
        UUID fondImageId,
        @Schema(description = "Fond étiré à la page entière (vrai, défaut) ou à sa taille au coin haut gauche. Null sans fond.")
        Boolean fondPleinePage) {

    /** Sans fond de page. */
    public MiseEnPageDto(String format, Integer margeHaut, Integer margeBas, Integer margeGauche, Integer margeDroite,
                         String enTete, String piedDePage) {
        this(format, margeHaut, margeBas, margeGauche, margeDroite, enTete, piedDePage, null, null);
    }
}
