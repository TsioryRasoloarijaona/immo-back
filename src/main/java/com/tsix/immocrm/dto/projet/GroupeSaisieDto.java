package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Groupe d'un bordereau envoyé à l'enregistrement.")
public record GroupeSaisieDto(
        @Schema(description = "Id du groupe : existant = modifié, inconnu = inséré (UUID généré par le front).") UUID id,
        @Schema(description = "Groupe parent, du même envoi ; null à la racine.") UUID parentId,
        @Schema(description = "Ordre (Sorting), obligatoire.") Integer ordre,
        @Schema(description = "Code (255 caractères au plus).") String code,
        @Schema(description = "Description, obligatoire (255 caractères au plus).") String description,
        @Schema(description = "Notes (1 000 caractères au plus).") String notes
) {
}
