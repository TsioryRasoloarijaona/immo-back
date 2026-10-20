package com.tsix.immocrm.dto.suiviaffectations;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Statut de dossier de vente proposé au filtre `statut` du suivi des affectations.")
public record StatutSuiviAffectationsDto(
        @Schema(description = "StrStatus.Code, à passer dans `?statut=`.", example = "RESERVATION") String code,
        @Schema(description = "StrStatus.Description.", example = "Réservation") String libelle) {
}
