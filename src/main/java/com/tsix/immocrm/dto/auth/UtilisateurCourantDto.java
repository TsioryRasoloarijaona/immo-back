package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Le compte authentifié — renvoyé par POST /api/auth/connexion et "
        + "GET /api/mon-compte.")
public record UtilisateurCourantDto(

        UUID id,

        @Schema(description = "Nom d'utilisateur (AdmUser.Code).")
        String username,

        String description,

        String email,

        @Schema(description = "Id de la fiche employé/commercial (CfgTier) liée, si connue.")
        UUID employeeId,

        @Schema(description = "Codes des rôles assignés (AdmRole.Code), ex: [\"Commercial\", \"Administrateur\"].")
        List<String> roles,

        @Schema(description = "true si un mot de passe temporaire est actif — le front doit "
                + "rediriger vers l'écran de changement de mot de passe ; toutes les autres "
                + "routes de l'API renvoient 403 tant que c'est le cas.")
        boolean mustChangePassword,

        @Schema(description = "false tant que le formulaire de première connexion (infos "
                + "personnelles) n'a pas été soumis — mêmes conséquences que mustChangePassword.")
        boolean profileCompleted
) {
}
