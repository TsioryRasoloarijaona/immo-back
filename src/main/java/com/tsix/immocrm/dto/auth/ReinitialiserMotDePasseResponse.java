package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Résultat d'une réinitialisation de mot de passe par un administrateur.")
public record ReinitialiserMotDePasseResponse(

        UUID admUserId,

        @Schema(description = "Nom d'utilisateur (AdmUser.Code) du compte réinitialisé.")
        String username,

        @Schema(description = "Mot de passe temporaire généré, à communiquer à l'utilisateur par "
                + "un canal hors-bande (jamais renvoyé une seconde fois — ni stocké en clair côté "
                + "serveur). Le compte devra le changer dès sa prochaine connexion "
                + "(mustChangePassword=true).")
        String motDePasseTemporaire
) {
}
