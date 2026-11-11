package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Type de lot par défaut (AdmParamsGuid « DefaultMarketType »), onglet « Valeurs par défaut ».")
public record TypeLotParDefautDto(
        @Schema(description = "Type de lot (PrjMarketType.PKey) ; null = aucun type par défaut.") UUID typeLotId
) {
}
