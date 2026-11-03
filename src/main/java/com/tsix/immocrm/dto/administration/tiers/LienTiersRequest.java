package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Contact à lier à un tiers.")
public record LienTiersRequest(
        @Schema(description = "Tiers lié, de tout type.", requiredMode = Schema.RequiredMode.REQUIRED) UUID contactId,
        @Schema(description = "Type de relation (CfgRelationType), facultatif.") UUID relationTypeId) {
}
