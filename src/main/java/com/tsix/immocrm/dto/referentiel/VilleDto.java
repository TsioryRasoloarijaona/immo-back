package com.tsix.immocrm.dto.referentiel;

import java.util.UUID;

/** {@code AdmCity} row. */
public record VilleDto(
        UUID id,
        String code,
        String description,
        UUID paysId
) {
}
