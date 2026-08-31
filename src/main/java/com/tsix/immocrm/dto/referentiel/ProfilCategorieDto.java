package com.tsix.immocrm.dto.referentiel;

import java.util.List;
import java.util.UUID;

/** {@code ComProspectionProfileCategory}, with nested profils — renders the "Profils" tab. */
public record ProfilCategorieDto(
        UUID id,
        String code,
        String description,
        Boolean multiSelect,
        List<ProfilDto> profils
) {
}
