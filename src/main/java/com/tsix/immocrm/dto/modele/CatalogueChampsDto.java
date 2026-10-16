package com.tsix.immocrm.dto.modele;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Catalogue des champs d'une catégorie de modèles, défini dans le code.")
public record CatalogueChampsDto(
        @Schema(description = "Code de la catégorie.", example = "DOSSIER_VENTE")
        String categorie,
        @Schema(description = "Groupes de champs simples.")
        List<GroupeChampsDto> groupes,
        @Schema(description = "Listes répétables (↻).")
        List<ListeChampsDto> listes) {
}
