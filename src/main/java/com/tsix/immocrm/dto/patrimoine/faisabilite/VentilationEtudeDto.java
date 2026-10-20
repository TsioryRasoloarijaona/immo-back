package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Quantité d'une unité de l'étude ventilée sur une tranche (StkFeasibilityStudyCfgTrancheStkBaseUnit).")
public record VentilationEtudeDto(
        @Schema(description = "PKey de la ligne (lecture ; ignoré en écriture, les ventilations sont recréées).") UUID id,
        @Schema(description = "Ligne d'unité de CETTE étude (LigneUniteEtudeDto.id), obligatoire.") UUID uniteEtudeId,
        @Schema(description = "Quantité ventilée ; par unité, la somme sur les tranches doit égaler sa quantité.")
        BigDecimal quantite,
        @Schema(description = "Poids (fraction), stocké tel quel.") BigDecimal poids) {
}
