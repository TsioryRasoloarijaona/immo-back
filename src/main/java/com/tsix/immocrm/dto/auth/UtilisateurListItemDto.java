package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UtilisateurListItemDto(

        UUID id,

        @Schema(description = "Nom d'utilisateur (AdmUser.Code).")
        String username,

        String description,

        String email,

        String phone,

        String mobile,

        @Schema(description = "true si le compte est désactivé (AdmUser.Disabled) — ne peut plus se connecter.")
        boolean disabled,

        @Schema(description = "Id de la fiche employé/commercial (CfgTier) liée, si connue.")
        UUID employeeId,

        @Schema(description = "Prénom de la fiche employé liée (CfgTier.FirstName), si connue.")
        String employeePrenom,

        @Schema(description = "Nom de la fiche employé liée (CfgTier.LastName), si connue.")
        String employeeNom,

        @Schema(description = "Code du type d'employé (CfgTierKind.Code, ex: \"COM\" pour commercial), si la fiche employé en a un.")
        String typeEmployeCode,

        @Schema(description = "Libellé du type d'employé (CfgTierKind.Description).")
        String typeEmployeLibelle,

        @Schema(description = "Codes des rôles assignés (AdmRole.Code), ex: [\"Commercial\", \"Administrateur\"]. Vide si aucun rôle assigné.")
        List<String> roles,

        boolean compteWebInitialise,

        boolean mustChangePassword,

        boolean profileCompleted,

        @Schema(description = "Date de création du compte (AdmUser.UserCreateDateTime).")
        LocalDateTime dateCreation,

        @Schema(example = "2")
        int nombreProjets,

        @Schema(description = "AdmUser.IsMobileUser.")
        boolean accesMobile,

        @Schema(description = "AdmUser.IsVisibleForChat.")
        boolean accesMessagerie,

        @Schema(description = "AdmUser.UserCreate.")
        String creePar,

        @Schema(description = "AdmUser.UserUpdate.")
        String modifiePar,

        @Schema(description = "AdmUser.UserUpdateDateTime.")
        LocalDateTime modifieLe
) {
}
