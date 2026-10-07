package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CompleterProfilRequest(

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String prenom,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String nom,

        @Schema(description = "Numéro de mobile.", requiredMode = Schema.RequiredMode.REQUIRED)
        String telephone,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime dateNaissance,

        @Schema(description = "Optionnelle — non requise pour marquer le profil comme complété.")
        String adresse
) {
}
