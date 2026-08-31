package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

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

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
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
