package com.tsix.immocrm.dto.patrimoine.plans;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Plan d'étage d'une tranche (StkHierarchyPlan), sans l'image : elle se lit par /api/plans/{planId}/image.")
public record PlanDto(
        @Schema(description = "PKey du plan.") UUID id,
        @Schema(description = "Étage du plan (StkFloor.PKey).") UUID etageId,
        @Schema(description = "Code de l'étage (« RDC », « Etage 1 »).") String etageCode,
        @Schema(description = "Code du plan (posé à l'import = code de l'étage, gardé ensuite).") String code,
        @Schema(description = "Largeur de l'image, repère des tracés (viewBox).") BigDecimal largeur,
        @Schema(description = "Hauteur de l'image, repère des tracés (viewBox).") BigDecimal hauteur,
        @Schema(description = "Abscisse de l'image dans le SVG d'origine (0 le plus souvent).") BigDecimal x,
        @Schema(description = "Ordonnée de l'image dans le SVG d'origine (0 le plus souvent).") BigDecimal y,
        @Schema(description = "Nombre de tracés d'articles de la tranche sur cet étage.") long nombreTraces
) {
}
