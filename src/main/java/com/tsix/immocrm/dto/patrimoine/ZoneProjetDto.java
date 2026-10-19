package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Zone d'un programme (CfgProjectZone), ligne d'un arbre rendu à plat.")
public record ZoneProjetDto(
        UUID id,
        @Schema(description = "Zone parente ; null = racine.") UUID parentId,
        @Schema(description = "Profondeur : 0 à la racine, parent + 1 en dessous.") Integer niveau,
        @Schema(description = "Rang parmi les zones du même parent.") Integer sorting,
        String code,
        String description,
        String notes,
        @Schema(description = "Vrai si une image (ImageData) est enregistrée ; non modifiable ici.") boolean aUneImage
) {
}
