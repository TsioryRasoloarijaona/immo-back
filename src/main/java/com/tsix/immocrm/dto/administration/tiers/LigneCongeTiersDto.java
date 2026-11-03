package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Congé du personnel (GrhVacation.GrhEmployeeId), sans montant ni salaire.")
public record LigneCongeTiersDto(
        @Schema(description = "PKey du congé.") UUID id,
        @Schema(description = "Code.") String code,
        @Schema(description = "Libellé du type de congé.") String type,
        @Schema(description = "Début (DateFrom), clé du tri.") LocalDateTime du,
        @Schema(description = "Fin (DateTo).") LocalDateTime au,
        @Schema(description = "Nombre de jours (DaysNumber).") BigDecimal jours,
        @Schema(description = "Libellé du statut.") String statut,
        @Schema(description = "Notes.") String notes) implements LigneOngletTiers {
}
