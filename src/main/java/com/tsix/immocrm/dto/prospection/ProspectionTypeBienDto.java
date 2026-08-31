package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/** {@code ComProspectionType} row. */
public record ProspectionTypeBienDto(
        UUID id,
        UUID typeBienId,
        String typeBien
) {
}
