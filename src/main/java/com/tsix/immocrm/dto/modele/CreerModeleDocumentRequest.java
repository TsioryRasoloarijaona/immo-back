package com.tsix.immocrm.dto.modele;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Création d'un modèle : enregistre sa version 1.")
public record CreerModeleDocumentRequest(
        @Schema(description = "Code de la catégorie (voir `GET /categories`).", example = "DOSSIER_VENTE")
        String categorie,
        @Schema(description = "Code unique dans la catégorie : majuscules, chiffres et `_`, 60 caractères au plus.",
                example = "LETTRE_BIENVENUE")
        String code,
        @Schema(description = "Nom affiché, 200 caractères au plus.", example = "Lettre de bienvenue")
        String libelle,
        @Schema(description = "Rang dans le menu « Imprimer ». Absent = 100.", example = "20")
        Integer ordre,
        @Schema(description = "HTML restreint du document (règles : FEATURE-modeles-documents.md §5).")
        String contenu,
        @Schema(description = "Mise en page. Absente = A4, marges de 15 mm, sans en-tête ni pied.")
        MiseEnPageDto miseEnPage) {
}
