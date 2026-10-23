package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Onglet Bilan : arbre des prévisions, totaux des racines et bilan de l'étude.")
public record BilanFaisabilitePrevisionsDto(
        @Schema(description = "Nœuds de l'arbre à plat.") List<LignePrevisionDto> lignes,
        @Schema(description = "Totaux des deux racines.") TotauxPrevisionsDto totaux,
        @Schema(description = "Bilan de l'étude (mêmes noms que la fiche).") BilanEtudeDto bilan) {
}
