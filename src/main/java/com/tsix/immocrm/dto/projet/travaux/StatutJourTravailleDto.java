package com.tsix.immocrm.dto.projet.travaux;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Statut (StrStatus) de l'entité « Jour travaillé ».")
public record StatutJourTravailleDto(
        @Schema(description = "StrStatus.PKey.") UUID id,
        @Schema(description = "Libellé (StrStatus.Description).", example = "En cours") String libelle,
        @Schema(description = "Couleur nommée OMRAN (StrStatus.ColorName), null si absente.", example = "Yellow") String couleur) {
}
