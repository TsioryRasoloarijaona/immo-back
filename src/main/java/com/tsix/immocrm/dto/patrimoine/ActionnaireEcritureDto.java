package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Création (POST) ou modification partielle (PATCH, champ absent = inchangé) d'un actionnaire.")
public record ActionnaireEcritureDto(
        @Schema(description = "Code, unique sur toute la table. Obligatoire à la création.")
        String code,
        @Schema(description = "Libellé. Obligatoire à la création.")
        String description,
        @Schema(description = "Notes (500 caractères au plus) ; chaîne vide = vider.")
        String notes
) {
}
