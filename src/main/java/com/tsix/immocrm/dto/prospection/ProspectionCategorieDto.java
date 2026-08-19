package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/** {@code ComProspectionCategory} row. */
public record ProspectionCategorieDto(
        UUID id,
        UUID categorieId,
        String categorie
) {
}
