package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Aperçu de la trésorerie : écarts-types de remplacement (≥ 0,5) ; absent = valeur de l'étude.")
public record ApercuTresorerieDto(
        @Schema(description = "Écart-type des travaux.") BigDecimal ecartTypeTravaux,
        @Schema(description = "Écart-type de la concrétisation.") BigDecimal ecartTypeConcretisation,
        @Schema(description = "Écart-type de la finalisation.") BigDecimal ecartTypeFinalisation,
        @Schema(description = "Inclure crédits et remboursements (vrai si absent).") Boolean avecFinancement) {
}
