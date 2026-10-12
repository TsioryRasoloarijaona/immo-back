package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Devise (StlCurrency) proposée sur la fiche d'une entité juridique.")
public record DeviseDto(
        @Schema(description = "Identifiant (StlCurrency.PKey).")
        UUID id,
        @Schema(description = "Code (MAD, EUR…).")
        String code,
        @Schema(description = "Libellé.")
        String description,
        @Schema(description = "Devise par défaut d'OMRAN (paramètre DefaultCurrency) : à présélectionner à la création.")
        boolean parDefaut
) {
}
