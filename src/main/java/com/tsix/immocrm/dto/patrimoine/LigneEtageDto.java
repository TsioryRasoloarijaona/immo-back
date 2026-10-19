package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Ligne de l'onglet Étages (CfgTrancheLevel) ; sans lien avec l'étage d'un article (StkFloor).")
public record LigneEtageDto(
        @Schema(description = "PKey de la ligne ; absent au PUT = nouvelle ligne.") UUID id,
        @Schema(description = "Niveau (LndLevel), obligatoire ; choix : /api/referentiels-patrimoine/niveaux-terrain.")
        UUID niveauTerrainId,
        @Schema(description = "Code du niveau.", accessMode = READ_ONLY) String niveauCode,
        @Schema(description = "Description du niveau.", accessMode = READ_ONLY) String niveauDescription
) implements LignesTrancheDto.Ligne {
}
