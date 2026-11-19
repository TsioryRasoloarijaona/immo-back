package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Statut stocké d'une demande interne d'achat (StrStatus).")
public record StatutDemandeAchat(
        @Schema(description = "StrStatus.PKey.") UUID id,
        @Schema(description = "StrStatus.Description.") String libelle,
        @Schema(description = "StrStatus.ColorName (nom de couleur .NET, ex. LimeGreen) ; null si absent.") String couleur) {
}
