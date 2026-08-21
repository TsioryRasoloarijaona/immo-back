package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corps de changement de mot de passe — utilisé aussi bien pour la "
        + "régularisation d'un mot de passe temporaire (première connexion) qu'un changement "
        + "volontaire ultérieur.")
public record ChangerMotDePasseRequest(

        @Schema(description = "Mot de passe actuel (temporaire ou non) — vérifié avant tout changement.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String ancienMotDePasse,

        @Schema(description = "Nouveau mot de passe, au moins 8 caractères.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String nouveauMotDePasse
) {
}
