package com.tsix.immocrm.dto.prospect;

import java.util.UUID;

/** {@code CfgTierContact} row, joined to the linked {@code CfgTier} for display. */
public record ContactProspectDto(
        UUID id,
        UUID contactProspectId,
        String nomContact,
        String telephoneContact,
        UUID relationTypeId,
        String relationType
) {
}
