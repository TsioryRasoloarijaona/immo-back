package com.tsix.immocrm.dto.dossier.sousarticles;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Sous-article à ajouter, choisi parmi `GET …/sous-articles/disponibles`.")
public record AjouterSousArticleRequest(
        @Schema(description = "Article du stock (`StkItem.PKey`).", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID articleId
) {
}
