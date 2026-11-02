package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat de « Déverrouiller tout ».")
public record ResultatDeverrouillageDto(
        @Schema(description = "Nombre de fiches déverrouillées ; 0 si aucun verrou n'était actif.") int nombre) {
}
