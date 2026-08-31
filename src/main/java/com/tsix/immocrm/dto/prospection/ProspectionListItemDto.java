package com.tsix.immocrm.dto.prospection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary shape for {@code GET /api/prospections} and {@code GET /api/pre-prospections}.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code ComProspection}. Adjust once a spec is available.
 */
public record ProspectionListItemDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        String tempDescription,
        UUID prospectId,
        String prospectNom,
        UUID commercialId,
        String commercialNom,
        String origine,
        String etape,
        Boolean fermee,
        BigDecimal prix,
        BigDecimal surface,
        UUID trancheId,
        // -- Libellé de la tranche principale (CfgTranche.Code/Description), pour affichage sans
        // second appel à GET /api/tranches/{trancheId}.
        String trancheCode,
        String trancheDescription
) {
}
