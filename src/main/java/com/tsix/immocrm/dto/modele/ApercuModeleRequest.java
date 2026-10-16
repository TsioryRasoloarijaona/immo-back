package com.tsix.immocrm.dto.modele;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Aperçu d'un contenu non enregistré, rempli avec une fiche réelle.")
public record ApercuModeleRequest(
        @Schema(description = "HTML restreint à prévisualiser (mêmes règles que l'enregistrement).")
        String contenu,
        @Schema(description = "Mise en page à prévisualiser. Absente = valeurs par défaut.")
        MiseEnPageDto miseEnPage,
        @Schema(description = "Fiche source (pour `DOSSIER_VENTE` : ComFolder.PKey), dans le périmètre de l'appelant.")
        UUID ficheId) {
}
