package com.tsix.immocrm.dto.patrimoine.relances;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bilan de l'écriture des premières relances.")
public record ResultatPremieresRelancesDto(
        @Schema(description = "Dossiers dont Revival1 et Revival1ValidityDate ont été écrits.", example = "12")
        int dossiersModifies,
        @Schema(description = "Alertes système créées (ComActionMessage + AdmAlertData).", example = "9")
        int alertesCreees) {
}
