package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Retenue d'un décompte (PrjWorkingOutRetainedLine).")
public record RetenueDecompteDto(
        @Schema(description = "PrjWorkingOutRetainedLine.PKey.") UUID id,
        @Schema(description = "Libellé (Description).") String libelle,
        @Schema(description = "Montant (Amount).") BigDecimal montant
) {
}
