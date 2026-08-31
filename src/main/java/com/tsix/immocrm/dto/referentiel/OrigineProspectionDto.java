package com.tsix.immocrm.dto.referentiel;

import java.util.UUID;

/** {@code ComProspectionOrigin} row. */
public record OrigineProspectionDto(
        UUID id,
        String code,
        String description,
        Boolean avecProposeur,
        Boolean avecCadeau,
        Boolean avecSupport
) {
}
