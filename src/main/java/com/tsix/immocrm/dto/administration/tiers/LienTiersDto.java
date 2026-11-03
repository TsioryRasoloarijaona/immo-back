package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Lien entre un tiers et un contact (CfgTierContact).")
public record LienTiersDto(
        @Schema(description = "PKey du lien.") UUID id,
        @Schema(description = "PKey du tiers lié.") UUID contactId,
        @Schema(description = "Libellé du tiers lié.") String contact,
        @Schema(description = "Type de relation (CfgRelationType) ; null si aucun.") UUID relationTypeId,
        @Schema(description = "Libellé du type de relation.") String relation) {
}
