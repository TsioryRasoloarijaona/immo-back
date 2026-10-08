package com.tsix.immocrm.dto.prospect;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** {@code contactProspectId} ou {@code nouveauContact}, exactement un des deux (400 sinon). */
public record CreerContactProspectRequest(
        @Schema(description = "Tiers (CfgTier) existant à lier comme contact. Alternative à "
                + "`nouveauContact`.")
        UUID contactProspectId,

        CreerProspectRequest nouveauContact,

        @Schema(description = "Type de relation (CfgRelationType → `CfgRelationTypeId`), p.ex. "
                + "« Conjoint », « Notaire ». **Obligatoire.**",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID relationTypeId
) {
}
