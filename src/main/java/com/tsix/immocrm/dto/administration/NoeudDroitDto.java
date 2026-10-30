package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nœud de l'arbre des droits (AdmRightEntity). L'ordre de la liste et la profondeur donnent la hiérarchie.")
public record NoeudDroitDto(
        @Schema(description = "Code du droit.") String code,
        @Schema(description = "Libellé du droit.") String description,
        @Schema(description = "Profondeur dans l'arbre.") Integer profondeur,
        @Schema(description = "Module, Form, Group ou Action.") String type) {
}
