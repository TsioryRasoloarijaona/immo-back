package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record ProjetListItemDto(
        UUID id,
        String code,
        String description,
        String ville,
        @Schema(description = "Code de l'entité (CfgCompany) du programme.") String entiteCode
) {
}
