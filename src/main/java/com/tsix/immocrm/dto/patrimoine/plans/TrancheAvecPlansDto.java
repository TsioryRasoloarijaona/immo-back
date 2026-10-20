package com.tsix.immocrm.dto.patrimoine.plans;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Tranche ayant au moins un plan (sélecteur de la page Plans).")
public record TrancheAvecPlansDto(
        @Schema(description = "PKey de la tranche.") UUID trancheId,
        @Schema(description = "Code de la tranche.") String code,
        @Schema(description = "Libellé de la tranche.") String description,
        @Schema(description = "Code du programme de la tranche ; null si sans programme.") String projetCode,
        @Schema(description = "Nombre de plans de la tranche.") long nombrePlans
) {
}
