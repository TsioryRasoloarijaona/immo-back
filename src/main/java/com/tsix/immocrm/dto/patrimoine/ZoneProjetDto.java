package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Zone d'un programme (CfgProjectZone), servie à plat : l'arbre se reconstruit par parentId.")
public record ZoneProjetDto(
        UUID id,
        @Schema(description = "Zone parente ; null pour une zone racine.")
        UUID parentId,
        String code,
        String description,
        @Schema(description = "Notes (1000 caractères au plus).")
        String notes,
        @Schema(description = "Profondeur dans l'arbre (Leveling) : 0 à la racine, calculée par le serveur.")
        Integer niveau,
        @Schema(description = "Ordre parmi les zones sœurs (Sorting).")
        Integer ordre,
        @Schema(description = "Vrai si la zone porte une image (ImageData, non exposée).")
        boolean aUneImage,
        String creePar,
        LocalDateTime creeLe,
        String modifiePar,
        LocalDateTime modifieLe
) {
}
