package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Cas « c'est bien cette personne » de {@code instructions/FEATURE-doublons-prospect-projet.md} §2. */
public record LierProspectRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UUID prospectId
) {
}
