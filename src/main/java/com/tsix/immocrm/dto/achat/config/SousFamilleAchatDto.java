package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Sous-famille d'achat proposée sur la fiche Article, avec sa famille (de quoi filtrer par famille).")
public record SousFamilleAchatDto(
        @Schema(description = "BuyItemUnderFamily.PKey.") UUID id,
        String code,
        String description,
        @Schema(description = "BuyItemFamily.PKey de sa famille.") UUID familleId,
        String familleCode,
        String familleDescription
) {
}
