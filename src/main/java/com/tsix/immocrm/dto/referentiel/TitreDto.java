package com.tsix.immocrm.dto.referentiel;

import java.util.UUID;

/** {@code AdmTierTitle} row (civilité). */
public record TitreDto(
        UUID id,
        String code,
        String description,
        UUID sexeId
) {
}
