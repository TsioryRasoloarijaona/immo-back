package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Rattachement d'un sous-article libre à un article.")
public record AffectationSousArticleDto(
        @Schema(description = "AlwaysAffected : le sous-article suit l'article dans la vente. Défaut faux.")
        Boolean toujoursAffecte
) {
}
