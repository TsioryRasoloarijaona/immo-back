package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Bloc de commission : montant fixe, ou pourcentage du prix de vente TTC.")
public record BlocCommissionDto(
        @Schema(description = "Vrai : commission = montant ; faux : pourcentage × prix TTC. Absent = vrai.")
        Boolean baseSurMontant,
        @Schema(description = "Pourcentage (fraction : 0.015 = 1,5 %) ; absent = 0.") BigDecimal pourcentage,
        @Schema(description = "Montant fixe en dirhams ; absent = 0.") BigDecimal montant
) {
}
