package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Nature (CfgTierKind) du type d'une famille.")
public record NatureTiersDto(
        @Schema(description = "PKey de la nature.") UUID id,
        @Schema(description = "Code.") String code,
        @Schema(description = "Libellé.") String libelle) {
}
