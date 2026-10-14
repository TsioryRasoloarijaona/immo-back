package com.tsix.immocrm.dto.dossier.liste;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Date proposée au filtre de période de la liste des dossiers.")
public record ChampDateListeDto(
        @Schema(description = "Valeur à passer dans `?champDate=`.", example = "CONCRETISATION")
        String code,
        @Schema(description = "Libellé affiché, celui d'OMRAN.", example = "Date concrétisation")
        String libelle) {
}
