package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Part d'un règlement imputée à une échéance du dossier (une `StlSettlementLine`).")
public record AffectationReglementRequest(

        @Schema(description = "Échéance du dossier (`ComDocument.PKey`). Une échéance d'un autre dossier donne 400.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID echeanceId,

        @Schema(example = "50000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal montant
) {
}
