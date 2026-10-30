package com.tsix.immocrm.dto.auth;

import com.tsix.immocrm.dto.administration.RestrictionsRoleDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche d'un compte AdmUser. Aucun mot de passe n'y figure.")
public record UtilisateurFicheDto(
        UUID id,

        @Schema(description = "Identifiant de connexion, immuable.")
        String code,

        String description,

        @Schema(description = "null si vide.")
        String email,

        @Schema(description = "null si vide.")
        String telephone,

        @Schema(description = "null si vide.")
        String mobile,

        @Schema(description = "CfgEmployeeId.")
        UUID personnelId,

        @Schema(description = "Libellé du tiers lié.")
        String personnelNom,

        boolean inactif,

        boolean avecNotification,

        boolean peutMasquerNotification,

        boolean accesMobile,

        String smtpServeur,

        Integer smtpPort,

        boolean smtpSsl,

        UUID cultureWebId,

        @Schema(description = "Ne donne aucun droit.")
        UUID roleWebId,

        List<UUID> roleIds,

        @Schema(description = "PKey des StlDeposit de l'utilisateur.")
        List<UUID> compteDepotIds,

        @Schema(description = "Calculées : une restriction est levée si au moins un rôle a false. Lecture seule.")
        RestrictionsRoleDto restrictions,

        @Schema(description = "true si des identifiants Arpent existent.")
        boolean compteWeb,

        String creePar,

        LocalDateTime creeLe,

        String modifiePar,

        LocalDateTime modifieLe
) {
}
