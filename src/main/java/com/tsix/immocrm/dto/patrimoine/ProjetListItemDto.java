package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ligne de la liste des programmes.")
public record ProjetListItemDto(
        UUID id,
        String code,
        String description,
        String ville,
        @Schema(description = "Code de l'entité juridique (CfgCompany.Code).")
        String entiteCode
) {
}
