package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Statut de réserve (BulObservationStatus).")
public record ObservationDto(
        @Schema(description = "BulObservationStatus.PKey.") UUID id,
        @Schema(description = "Description, à défaut le code.") String libelle,
        @Schema(description = "Couleur (ColorName), nom de couleur OMRAN.", example = "RED") String couleur,
        @Schema(description = "Statut fermé (IsColsed) : une décision fermée n'est pas reportée.") boolean ferme) {
}
