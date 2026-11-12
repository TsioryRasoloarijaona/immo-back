package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Entité juridique (CfgCompany) portant au moins un projet visible, pour le filtre « Entité » des lots.")
public record EntiteFiltreDto(
        @Schema(description = "CfgCompany.PKey.") UUID id,
        @Schema(description = "Code de l'entité.") String code,
        @Schema(description = "Libellé de l'entité (Description).") String libelle
) {
}
