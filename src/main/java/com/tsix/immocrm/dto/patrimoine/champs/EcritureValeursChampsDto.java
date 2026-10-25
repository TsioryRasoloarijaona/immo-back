package com.tsix.immocrm.dto.patrimoine.champs;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Valeurs à enregistrer pour un objet ; tout ou rien : une valeur refusée n'en écrit aucune.")
public record EcritureValeursChampsDto(
        @Schema(description = "Valeurs saisies ; un champ absent de la liste garde sa valeur.")
        List<ValeurChampDto> valeurs) {
}
