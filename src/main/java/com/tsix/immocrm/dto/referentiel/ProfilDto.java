package com.tsix.immocrm.dto.referentiel;

import java.util.UUID;

/** {@code ComProspectionProfile} row. */
public record ProfilDto(
        UUID id,
        String code,
        String description
) {
}
