package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne de ventilation du projet par tranche (PrjProjectCfgTrancheVentilation).")
public record VentilationTrancheDto(
        @Schema(description = "PrjProjectCfgTrancheVentilation.PKey.") UUID id,
        @Schema(description = "Tranche (CfgTranche.PKey).") UUID trancheId,
        @Schema(description = "Code de la tranche, null si la tranche n'existe plus.") String trancheCode,
        @Schema(description = "Libellé de la tranche.") String trancheDescription,
        @Schema(description = "Taux (VentilationRate), fraction : 0,25 = 25 %.", example = "0.25") BigDecimal taux
) {
}
