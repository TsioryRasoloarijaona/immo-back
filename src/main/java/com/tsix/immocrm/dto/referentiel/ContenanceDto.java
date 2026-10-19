package com.tsix.immocrm.dto.referentiel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Contenance (StkContenance) et ses options : option vide = case grisée ; quantité seulement si "
        + "optionNAvecQuantite.")
public record ContenanceDto(
        UUID id,
        String code,
        String description,
        @Schema(description = "Libellé de l'option 1, null si l'option n'existe pas.") String option1,
        boolean option1AvecQuantite,
        String option2,
        boolean option2AvecQuantite,
        String option3,
        boolean option3AvecQuantite
) {
}
