package com.tsix.immocrm.dto.remise;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Acceptation d'une demande de remise par le siège.")
public record AccepterRemiseRequest(
        @Schema(description = "Taux accordé en pourcentage, ≤ taux demandé ; absent = taux demandé.", example = "4",
                nullable = true)
        BigDecimal taux
) {
}
