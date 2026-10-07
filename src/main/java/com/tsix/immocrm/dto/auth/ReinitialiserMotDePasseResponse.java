package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record ReinitialiserMotDePasseResponse(

        @Schema(description = "Id de l'AdmUser réinitialisé.")
        UUID admUserId,

        @Schema(description = "Identifiant de connexion du compte — le login Omran (AdmUser.Code).")
        String username,

        @Schema(example = "Ab3xK9mQp2Rt")
        String motDePasseTemporaire,

        @Schema(example = "true")
        boolean emailEnvoye,

        @Schema(example = "j***@exemple.ma", nullable = true)
        String destinataire
) {
}
