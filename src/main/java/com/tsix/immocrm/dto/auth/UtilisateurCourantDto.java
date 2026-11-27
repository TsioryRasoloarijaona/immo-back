package com.tsix.immocrm.dto.auth;

import com.tsix.immocrm.dto.perimetre.ProjetPerimetreDto;
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

        boolean mustChangePassword,

        boolean profileCompleted,

        boolean perimetreRestreint,

        List<ProjetPerimetreDto> projetsAutorises,

        @Schema(description = "Vrai si le compte porte le rôle Agence (lu en base) : seuls les écrans agence lui sont ouverts.")
        boolean agence,

        @Schema(description = "Vrai si le compte peut décider les demandes de remise des agences (siège valideur, lu en base).")
        boolean validationRemise
) {
}
