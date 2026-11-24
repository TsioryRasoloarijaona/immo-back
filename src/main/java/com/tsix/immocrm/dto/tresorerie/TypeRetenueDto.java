package com.tsix.immocrm.dto.tresorerie;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Type de retenue à la source (StlDeductionType), tri par Sorting.")
public record TypeRetenueDto(
        UUID id,
        String code,
        @Schema(description = "Libellé du type.") String libelle,
        @Schema(description = "Taux proposé en fraction (RASValue, 0.1 = 10 %).", example = "0.1") BigDecimal taux,
        @Schema(description = "Année (RASYear) ; null ou −1 = toutes ; le front filtre sur l'année de la date pièce.",
                example = "2026")
        Integer anneeRas) {
}
