package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche d'un rôle.")
public record RoleDto(
        @Schema(description = "PKey du rôle.") UUID id,
        @Schema(description = "Code du rôle, immuable après création.") String code,
        @Schema(description = "Libellé, affiché tel quel.") String description,
        @Schema(description = "Les 10 restrictions, jamais null.") RestrictionsRoleDto restrictions,
        @Schema(description = "Codes du rôle présents dans l'arbre des droits.") List<String> codesDroits,
        @Schema(description = "Nombre de droits du rôle absents de l'arbre, conservés à chaque enregistrement.") long droitsHorsArbre,
        @Schema(description = "Vrai pour le rôle Administrateur : toute écriture répond 409.") boolean verrouille) {
}
