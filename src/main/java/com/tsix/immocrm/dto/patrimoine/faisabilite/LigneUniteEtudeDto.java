package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Unité de base d'une étude (StkFSBaseUnitStkFeasibilityStudy). En écriture : id connu = mise à jour, "
        + "sans id = création ; uniteCode et uniteLibelle sont ignorés.")
public record LigneUniteEtudeDto(
        @Schema(description = "PKey de la ligne ; dans /calcul, identifiant provisoire libre pour relier les ventilations.")
        UUID id,
        @Schema(description = "Ordre d'affichage.") Integer sorting,
        @Schema(description = "Unité de base (référentiel unites-de-base), obligatoire.") UUID uniteBaseId,
        @Schema(description = "Lecture : code de l'unité de base.") String uniteCode,
        @Schema(description = "Lecture : libellé de l'unité de base.") String uniteLibelle,
        @Schema(description = "Quantité (m² pour une unité de surface).") BigDecimal quantite) {
}
