package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Tranche obligatoire, plus strict que le légataire (qui n'impose rien) : c'est elle qui place la
 * prospection dans le périmètre commercial. Fournie ici ou déjà portée, sinon 400.
 */
public record TransformerProspectionRequest(
        @Schema(description = "Commercial (CfgTier) affecté à la prospection. Obligatoire.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "5ca4a41f-35d1-4e89-a521-4e9c363ac99b")
        UUID commercialId,

        @Schema(example = "5eee12f0-03ea-4008-9808-4109b6e242bf")
        UUID trancheId,

        @Schema(example = "null")
        UUID articleId
) {
}
