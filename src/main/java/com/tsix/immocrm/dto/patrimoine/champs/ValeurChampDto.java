package com.tsix.immocrm.dto.patrimoine.champs;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Valeur saisie pour un champ spécifique de l'objet.")
public record ValeurChampDto(
        @Schema(description = "Id de la définition (ChampSpecifiqueDto.champId), du même porteur.")
        UUID champId,
        @Schema(description = "Valeur en texte : date aaaa-mm-jj, décimale au point, pourcentage en fraction, true/false.",
                example = "1234.5")
        String valeur) {
}
