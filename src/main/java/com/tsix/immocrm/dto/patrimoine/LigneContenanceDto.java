package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Ligne de l'onglet Contenances de l'article (StkItemContenance).")
public record LigneContenanceDto(
        @Schema(description = "PKey de la ligne ; absent au PUT = nouvelle ligne.") UUID id,
        @Schema(description = "Contenance (StkContenance), obligatoire.") UUID contenanceId,
        @Schema(description = "ItemsNumber : nombre, ≥ 0 ; absent = 0.") Integer nombre,
        @Schema(description = "Option 1 cochée ; refusée (400) si l'option 1 de la contenance est vide.") Boolean avecOption1,
        @Schema(description = "Quantité de l'option 1 ; refusée (400) sans IsOption1WithQuantity.") Integer quantiteOption1,
        Boolean avecOption2,
        Integer quantiteOption2,
        Boolean avecOption3,
        Integer quantiteOption3,
        @Schema(description = "Code de la contenance.", accessMode = READ_ONLY) String contenanceCode,
        @Schema(description = "Description de la contenance.", accessMode = READ_ONLY) String contenanceDescription
) implements LignesTrancheDto.Ligne {
}
