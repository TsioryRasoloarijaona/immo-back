package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Saisie d'un rôle. En modification, champ absent ou null = inchangé.")
public record RoleEcritureDto(
        @Schema(description = "Création seulement : obligatoire, 255 caractères au plus, espaces de bord retirés. Ignoré en modification.")
        String code,
        @Schema(description = "Facultative, 255 caractères au plus ; une chaîne vide efface la description.") String description,
        @Schema(description = "Case absente : true à la création, inchangée en modification.") RestrictionsRoleDto restrictions) {
}
