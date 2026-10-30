package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Tiers de type Employé : coordonnées copiées dans la fiche si l'écran le propose (R11).")
public record PersonnelReferenceDto(
        UUID id,

        String code,

        String description,

        @Schema(description = "CfgTier.EMail, null si vide.")
        String email,

        @Schema(description = "CfgTier.Mobile, null si vide.")
        String mobile,

        @Schema(description = "CfgTier.Phone, null si vide.")
        String telephone
) {
}
