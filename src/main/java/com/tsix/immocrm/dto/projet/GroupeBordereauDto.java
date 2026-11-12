package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Groupe d'un bordereau (arbre plat), par ordre puis code.")
public record GroupeBordereauDto(
        @Schema(description = "PKey du regroupement.") UUID id,
        @Schema(description = "Groupe parent ; null à la racine.") UUID parentId,
        @Schema(description = "Ordre (Sorting).") Integer ordre,
        @Schema(description = "Code du groupe.") String code,
        @Schema(description = "Libellé du groupe.") String description,
        @Schema(description = "Groupe d'approvisionnement (IsStock).") boolean stock,
        @Schema(description = "Notes du groupe.") String notes
) {
}
