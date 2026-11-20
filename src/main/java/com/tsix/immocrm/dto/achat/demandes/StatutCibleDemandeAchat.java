package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Statut d'arrivée d'une transition (StrStatus).")
public record StatutCibleDemandeAchat(
        @Schema(description = "StrStatus.PKey.") UUID id,
        @Schema(description = "StrStatus.Code.") String code,
        @Schema(description = "StrStatus.Description.") String libelle,
        @Schema(description = "StrStatus.ColorName ; null si absent.") String couleur) {
}
