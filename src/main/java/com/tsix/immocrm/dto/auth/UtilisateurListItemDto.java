package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Une ligne de GET /api/utilisateurs — compte (AdmUser) avec ses "
        + "informations, sa fiche employé (CfgTier) et ses rôles (AdmRole), pour l'écran "
        + "d'administration des comptes.")
public record UtilisateurListItemDto(

        UUID id,

        @Schema(description = "Nom d'utilisateur (AdmUser.Code).")
        String username,

        String description,

        String email,

        String phone,

        String mobile,

        @Schema(description = "true si le compte est désactivé (AdmUser.Disabled) — ne peut plus "
                + "se connecter.")
        boolean disabled,

        @Schema(description = "Id de la fiche employé/commercial (CfgTier) liée, si connue.")
        UUID employeeId,

        @Schema(description = "Prénom de la fiche employé liée (CfgTier.FirstName), si connue.")
        String employeePrenom,

        @Schema(description = "Nom de la fiche employé liée (CfgTier.LastName), si connue.")
        String employeeNom,

        @Schema(description = "Code du type d'employé (CfgTierKind.Code, ex: \"COM\" pour "
                + "commercial), si la fiche employé en a un.")
        String typeEmployeCode,

        @Schema(description = "Libellé du type d'employé (CfgTierKind.Description).")
        String typeEmployeLibelle,

        @Schema(description = "Codes des rôles assignés (AdmRole.Code), ex: [\"Commercial\", "
                + "\"Administrateur\"]. Vide si aucun rôle assigné.")
        List<String> roles,

        @Schema(description = "true si des identifiants web (AppUserCredential) existent pour ce "
                + "compte — false si le compte legacy ne s'est encore jamais connecté à "
                + "l'application web et n'a jamais été initialisé par un administrateur "
                + "(voir POST /api/utilisateurs/{admUserId}/reinitialiser-mot-de-passe).")
        boolean compteWebInitialise,

        @Schema(description = "true si un mot de passe temporaire est actif pour ce compte "
                + "(en attente de changement à la prochaine connexion). false, y compris si "
                + "compteWebInitialise est false.")
        boolean mustChangePassword,

        @Schema(description = "true si le profil (informations personnelles de première connexion) "
                + "a déjà été complété. false, y compris si compteWebInitialise est false.")
        boolean profileCompleted,

        @Schema(description = "Date de création du compte (AdmUser.UserCreateDateTime).")
        LocalDateTime dateCreation
) {
}
