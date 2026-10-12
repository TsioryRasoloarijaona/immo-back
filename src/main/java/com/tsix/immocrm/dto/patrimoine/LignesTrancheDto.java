package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Liste complète d'un onglet de tranche : remplace la liste enregistrée (PUT).")
public record LignesTrancheDto<T>(
        @Schema(description = "Lignes voulues. Avec id connu : modifiée ; sans id : créée ; absente : supprimée.")
        List<T> lignes
) {
}
