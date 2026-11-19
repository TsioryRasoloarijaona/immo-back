package com.tsix.immocrm.services.workflow;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Identifiants Arpent d'un second utilisateur, exigés par une transition à second valideur ; jamais conservés.")
public record SecondValideur(
        @Schema(description = "Login (AdmUser.Code) du second utilisateur.") String login,
        @Schema(description = "Mot de passe Arpent du second utilisateur.", format = "password") String motDePasse) {

    @Override
    public String toString() {
        return "SecondValideur[login=" + login + "]";
    }
}
