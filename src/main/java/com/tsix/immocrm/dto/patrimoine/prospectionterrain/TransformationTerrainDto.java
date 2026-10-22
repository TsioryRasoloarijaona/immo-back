package com.tsix.immocrm.dto.patrimoine.prospectionterrain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Champs du terrain que la prospection ne porte pas (dialogue « Transformer en terrain »).")
public record TransformationTerrainDto(
        @Schema(description = "Société du terrain (CfgCompany active). Obligatoire.") UUID entiteId,
        @Schema(description = "Vocation du terrain (StkVocation). Obligatoire.") UUID vocationId,
        @Schema(description = "Catégorie de nature Terrain. Obligatoire.") UUID categorieId,
        @Schema(description = "Code du terrain, défaut = code de la prospection ; unique dans la société (409).") String code
) {
}
