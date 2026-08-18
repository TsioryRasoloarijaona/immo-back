package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary shape for {@code GET /api/dossiers}.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code ComFolder}. Adjust once a spec is available.
 */
public record DossierListItemDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        String statut,
        UUID clientId,
        String clientNom,
        UUID commercialId,
        String commercialNom,
        BigDecimal prix,
        BigDecimal totalAPayer
) {
}
