package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Droits cochés dans l'arbre.")
public record DroitsRoleEcritureDto(
        @Schema(description = "Obligatoire ; vide = aucun droit de l'arbre. Casse respectée, doublons traités une fois.") List<String> codes) {
}
