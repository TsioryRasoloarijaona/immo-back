package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Bordereau entier envoyé à l'enregistrement (hors approvisionnement) : la base est alignée sur cet envoi.")
public record BordereauSaisieDto(
        @Schema(description = "Tous les groupes du bordereau ; liste vide acceptée, null refusé.") List<GroupeSaisieDto> groupes,
        @Schema(description = "Toutes les lignes du bordereau ; liste vide acceptée, null refusé.") List<LigneSaisieDto> lignes
) {
}
