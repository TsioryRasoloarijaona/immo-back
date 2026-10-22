package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Crédits et débits d'un mois sur une tranche de l'étude.")
public record MontantTrancheDto(
        @Schema(description = "Code de la tranche de l'étude.") String trancheCode,
        @Schema(description = "Total des crédits du mois.") BigDecimal credit,
        @Schema(description = "Total des débits du mois.") BigDecimal debit) {
}
