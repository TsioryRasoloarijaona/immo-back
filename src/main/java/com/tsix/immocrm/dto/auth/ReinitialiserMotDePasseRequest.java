package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/** Voir instructions/FEATURE-acces-arpent-depuis-omran.md B1. */
public record ReinitialiserMotDePasseRequest(

        @Schema(example = "j.dupont@exemple.ma", nullable = true)
        String email
) {
}
