package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corps de connexion : identifiants d'un compte AdmUser.")
public record ConnexionRequest(

        @Schema(description = "Nom d'utilisateur (AdmUser.Code).", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @Schema(description = "Mot de passe web (jamais le mot de passe legacy AdmUser.Pass).", requiredMode = Schema.RequiredMode.REQUIRED)
        String password
) {
}
