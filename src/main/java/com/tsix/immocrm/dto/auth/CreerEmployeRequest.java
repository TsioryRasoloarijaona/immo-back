package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "Corps de la création d'un nouvel employé par un administrateur — crée en "
        + "une fois la fiche personne (CfgTier), le compte de connexion (AdmUser) et ses "
        + "identifiants web.")
public record CreerEmployeRequest(

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String prenom,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String nom,

        @Schema(description = "Nom d'utilisateur du compte (AdmUser.Code) — doit être unique.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "j.dupont")
        String identifiant,

        // Seul champ du record porteur de contraintes, et c'est délibéré : la création écrit
        // cette adresse sur CfgTier.EMail ET AdmUser.Email, puis s'en sert pour expédier le mot
        // de passe temporaire. Une adresse absente ou malformée se propageait jusque-là sur deux
        // tables sans que rien ne la vérifie — le @Schema l'annonçait REQUIRED, mais @Valid
        // n'avait aucune contrainte à faire respecter. Durcir les autres champs déborderait de
        // cette feature.
        @Schema(description = "Adresse email de l'employé, obligatoire et vérifiée : elle sert à "
                + "lui expédier son mot de passe temporaire.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "j.dupont@exemple.ma")
        @NotBlank(message = "email est requis")
        @Email(message = "email doit être une adresse valide")
        String email,

        String mobile,

        @Schema(description = "Code CfgTierKind qualifiant le type d'employé (ex: COM pour "
                + "commercial). Doit exister en base.", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "COM")
        String typeEmployeCode,

        @Schema(description = "Codes AdmRole à assigner au compte (ex: [\"Commercial\"]) — au "
                + "moins un requis, chaque code doit exister en base.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> roles
) {
}
