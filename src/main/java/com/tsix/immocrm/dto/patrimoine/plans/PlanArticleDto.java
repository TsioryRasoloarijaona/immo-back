package com.tsix.immocrm.dto.patrimoine.plans;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Plan sur lequel un article est tracé (onglet Plan de la fiche article).")
public record PlanArticleDto(
        @Schema(description = "PKey du plan ; image et tracés par /api/plans/{planId}/image et /traces.") UUID planId,
        @Schema(description = "Tranche du plan (celle de l'article).") UUID trancheId,
        @Schema(description = "Étage du tracé (StkFloor.PKey), qui peut différer de l'étage de l'article.") UUID etageId,
        @Schema(description = "Code de l'étage du tracé.") String etageCode,
        @Schema(description = "Largeur de l'image, repère des tracés (viewBox).") BigDecimal largeur,
        @Schema(description = "Hauteur de l'image, repère des tracés (viewBox).") BigDecimal hauteur,
        @Schema(description = "Abscisse de l'image dans le SVG d'origine.") BigDecimal x,
        @Schema(description = "Ordonnée de l'image dans le SVG d'origine.") BigDecimal y
) {
}
