package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Niveau de construction autorisé pour la tranche (CfgTrancheLevel). Sans lien avec l'étage d'un article.")
public record EtageTrancheDto(
        @Schema(description = "Id de la ligne (CfgTrancheLevel.PKey).")
        UUID id,
        @Schema(description = "Niveau (LndLevel.id).")
        UUID niveauTerrainId,
        @Schema(description = "Code du niveau, lecture seule.")
        String niveauCode,
        @Schema(description = "Libellé du niveau, lecture seule.")
        String niveauDescription
) {
}
