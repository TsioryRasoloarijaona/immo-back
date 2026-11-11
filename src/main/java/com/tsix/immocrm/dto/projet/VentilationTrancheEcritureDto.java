package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Saisie d'une ligne de ventilation par tranche (ajout ou remplacement).")
public record VentilationTrancheEcritureDto(
        @Schema(description = "Tranche de la société du projet, pas déjà ventilée sur ce projet. Obligatoire.",
                requiredMode = Schema.RequiredMode.REQUIRED) UUID trancheId,
        @Schema(description = "Taux en fraction, obligatoire ; Σ des taux du projet arrondie à 2 décimales ≤ 1.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "0.25") BigDecimal taux
) {
}
