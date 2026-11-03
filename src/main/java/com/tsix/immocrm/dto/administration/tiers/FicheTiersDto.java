package com.tsix.immocrm.dto.administration.tiers;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Fiche d'un tiers : champs de EcritureTiersRequest à plat, plus code, trace et libellés.")
public record FicheTiersDto(
        @Schema(description = "PKey du tiers.") UUID id,
        @Schema(description = "Code issu du compteur des tiers, jamais modifié.") String code,
        @Schema(description = "Description, égale à la raison sociale à chaque écriture.") String description,
        @Schema(description = "Auteur de la création.") String creePar,
        @Schema(description = "Date de création.") LocalDateTime creeLe,
        @Schema(description = "Auteur de la dernière modification.") String modifiePar,
        @Schema(description = "Date de la dernière modification.") LocalDateTime modifieLe,
        @JsonUnwrapped EcritureTiersRequest champs,
        @Schema(description = "Libellé de la nature.") String nature,
        @Schema(description = "Libellé du pays.") String pays,
        @Schema(description = "Libellé de la forme sociale.") String formeSociale,
        @Schema(description = "Libellé de la devise.") String devise) {
}
