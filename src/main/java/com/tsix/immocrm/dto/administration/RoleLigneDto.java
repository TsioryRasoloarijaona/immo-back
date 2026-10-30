package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ligne de la liste des rôles.")
public record RoleLigneDto(
        @Schema(description = "PKey du rôle.") UUID id,
        @Schema(description = "Code du rôle, porté par les autorisations.") String code,
        @Schema(description = "Libellé, affiché tel quel.") String description,
        @Schema(description = "Nombre de lignes AdmUserRole ; le rôle web des comptes n'y entre pas.") long utilisateurs,
        @Schema(description = "Nombre de lignes AdmRight, droits hors de l'arbre compris.") long droits) {
}
