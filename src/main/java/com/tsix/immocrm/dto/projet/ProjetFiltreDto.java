package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Projet (PrjProject) visible de l'appelant, proposé dans le filtre « Projet » des lots et décomptes.")
public record ProjetFiltreDto(
        @Schema(description = "PrjProject.PKey.") UUID id,
        @Schema(description = "Code du projet.", example = "ARS-PRJ") String code,
        @Schema(description = "Libellé du projet.") String description,
        @Schema(description = "Nombre de lots (PrjMarket) du projet.") long nbLots,
        @Schema(description = "Entité juridique du projet (CfgCompany.PKey), null si absente.") UUID entiteId
) {
}
