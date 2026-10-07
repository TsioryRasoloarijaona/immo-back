package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.util.List;

/** Schéma de financement simulé, non persisté. */
public record SchemaFinancementDto(
        List<LigneSchemaFinancementDto> lignes,
        BigDecimal totalGeneral
) {
}
