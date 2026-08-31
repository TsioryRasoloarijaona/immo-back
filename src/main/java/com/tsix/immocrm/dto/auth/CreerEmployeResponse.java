package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Résultat de la création d'un employé par un administrateur.")
public record CreerEmployeResponse(

        @Schema(description = "Id de la fiche personne créée (CfgTier).")
        UUID cfgTierId,

        @Schema(description = "Id du compte de connexion créé (AdmUser).")
        UUID admUserId,

        @Schema(description = "Nom d'utilisateur (AdmUser.Code) du compte créé.")
        String identifiant,

        @Schema(description = "Codes AdmRole assignés au compte.")
        List<String> roles,

        @Schema(description = "Mot de passe temporaire généré, à communiquer à l'employé par un "
                + "canal hors-bande (jamais renvoyé une seconde fois — ni stocké en clair côté "
                + "serveur). Le compte devra le changer dès sa première connexion "
                + "(mustChangePassword=true).")
        String motDePasseTemporaire
) {
}
