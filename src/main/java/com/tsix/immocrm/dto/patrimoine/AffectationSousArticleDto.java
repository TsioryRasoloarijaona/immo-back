package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corps facultatif de l'affectation d'un sous-article libre à un article.")
public record AffectationSousArticleDto(
        @Schema(description = "Toujours affecté (AlwaysAffected). Absent = false.")
        Boolean toujoursAffecte
) {
}
