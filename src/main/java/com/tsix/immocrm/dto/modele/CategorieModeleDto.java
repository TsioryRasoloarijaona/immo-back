package com.tsix.immocrm.dto.modele;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Catégorie de modèles = type de fiche source.")
public record CategorieModeleDto(
        @Schema(description = "Code de la catégorie.", example = "DOSSIER_VENTE")
        String code,
        @Schema(description = "Libellé français.", example = "Dossier de vente")
        String libelle) {
}
