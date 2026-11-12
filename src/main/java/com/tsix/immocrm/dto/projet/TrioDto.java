package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Montant HT, TVA et TTC, arrondis à 2 décimales.")
public record TrioDto(
        @Schema(description = "Montant hors taxes.") BigDecimal ht,
        @Schema(description = "TVA (TTC − HT).") BigDecimal tva,
        @Schema(description = "Montant toutes taxes comprises.") BigDecimal ttc
) {
}
