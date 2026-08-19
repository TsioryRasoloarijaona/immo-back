package com.tsix.immocrm.dto.referentiel;

import java.util.UUID;

/** Generic id/code/description lookup row, reused across most simple reference tables. */
public record ReferentielItemDto(
        UUID id,
        String code,
        String description,
        Integer sorting
) {
}
