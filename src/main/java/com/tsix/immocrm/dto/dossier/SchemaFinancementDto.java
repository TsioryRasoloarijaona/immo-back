package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/** Schéma de financement simulé, non persisté. */
public record SchemaFinancementDto(
        List<LigneSchemaFinancementDto> lignes,
        BigDecimal totalGeneral,
        @Schema(example = "1200000.00")
        BigDecimal prixAvantRemise,
        @Schema(example = "60000.00")
        BigDecimal montantRemise,
        @Schema(example = "5")
        BigDecimal remisePercent,
        @Schema(example = "1140000.00")
        BigDecimal prixApresRemise
) {
}
