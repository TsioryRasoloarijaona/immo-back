package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Sous-fenêtre Avances du lot : deux grilles, par date décroissante puis code.")
public record AvancesLotDto(
        @Schema(description = "Avances sur marché (ReferenceType MARKET_ADVANCE).") List<AvanceLotDto> surMarche,
        @Schema(description = "Avances sur décompte (ReferenceType WORKINGOUT_ADVANCE).") List<AvanceLotDto> surDecompte
) {
}
