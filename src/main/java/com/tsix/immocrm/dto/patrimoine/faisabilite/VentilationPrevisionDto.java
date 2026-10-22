package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Part d'un nœud du bilan sur une tranche : total × poids de la tranche.")
public record VentilationPrevisionDto(
        @Schema(description = "Tranche de l'étude (StkFeasibilityStudyCfgTranche.PKey).") UUID trancheEtudeId,
        @Schema(description = "Code de la tranche de l'étude.") String trancheCode,
        @Schema(description = "Total HT ventilé.") BigDecimal montantHt,
        @Schema(description = "Total TTC ventilé.") BigDecimal montant) {
}
