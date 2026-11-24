package com.tsix.immocrm.dto.tresorerie;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne de retenue à la source d'un décaissement (StlDeduction).")
public record LigneRetenueDto(
        @Schema(description = "StlDeduction.PKey ; ignoré en entrée (le PUT remplace toutes les lignes), rendu en sortie.")
        UUID id,
        @Schema(description = "Type de retenue (StlDeductionType.PKey).") UUID typeRetenueId,
        @Schema(description = "Base de la retenue (AmountBase).", example = "1000") BigDecimal base,
        @Schema(description = "Taux en fraction (RsRatio, 0.1 = 10 %) ; ignoré si montantRs est saisi.", example = "0.1")
        BigDecimal taux,
        @Schema(description = "Montant retenu (AmountRS) ; saisi, il pilote le taux (montantRs / base).", example = "100")
        BigDecimal montantRs,
        @Schema(description = "Sortie seule : base − montantRs, arrondi à 2 décimales.", example = "900")
        BigDecimal montantNet,
        @Schema(description = "Notes de la ligne.") String notes) {
}
