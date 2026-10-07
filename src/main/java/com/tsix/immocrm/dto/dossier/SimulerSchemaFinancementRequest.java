package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Les champs de {@link CreerDossierRequest} qui entrent dans le calcul, même sémantique : la simulation
 * doit produire exactement les montants de la création. Le premier article fait foi (tranche → barème
 * AVANCE/SYNDIC, vocation) ; sans {@code prix}, pas de schéma.
 */
public record SimulerSchemaFinancementRequest(
        List<UUID> articleIds,
        @Schema(example = "1000000.00")
        BigDecimal prix,
        @Schema(example = "10")
        BigDecimal remisePercent
) {
}
