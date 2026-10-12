package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ligne de répartition d'une entité juridique (CfgCompanyShareholder).")
public record LigneRepartitionDto(
        @Schema(description = "Identifiant de la ligne (CfgCompanyShareholder.PKey).")
        UUID id,
        @Schema(description = "Actionnaire (CfgShareholder.PKey).")
        UUID actionnaireId,
        @Schema(description = "Code de l'actionnaire.")
        String actionnaireCode,
        @Schema(description = "Libellé de l'actionnaire.")
        String actionnaireLibelle,
        @Schema(description = "Nombre de parts (actions) détenues, entier.")
        Integer nombreParts
) {
}
