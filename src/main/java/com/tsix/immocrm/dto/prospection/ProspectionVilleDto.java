package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/** {@code ComProspectionCity} row. */
public record ProspectionVilleDto(
        UUID id,
        UUID villeId,
        String ville
) {
}
