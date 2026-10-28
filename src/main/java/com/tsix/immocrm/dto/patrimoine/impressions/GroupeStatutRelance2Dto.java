package com.tsix.immocrm.dto.patrimoine.impressions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Compteurs de la Relance 2 pour un statut présent dans la tranche.")
public record GroupeStatutRelance2Dto(
        @Schema(description = "Statut du dossier (StrStatus).")
        StatutRelance2Dto statut,
        @Schema(description = "Dossiers retenus à ce statut, écartés compris.", example = "73")
        int dossiers,
        @Schema(description = "Lettres imprimées pour ce statut (écartés exclus).", example = "81")
        int lettres) {

    @Schema(description = "Statut de dossier (StrStatus).")
    public record StatutRelance2Dto(
            @Schema(description = "StrStatus.PKey.") UUID id,
            @Schema(description = "StrStatus.Code, à passer dans `?statut=`.", example = "CONCRETISATION") String code,
            @Schema(description = "StrStatus.Description.", example = "CONCRETISATION") String description) {
    }
}
