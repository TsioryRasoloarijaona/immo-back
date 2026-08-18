package com.tsix.immocrm.dto.patrimoine;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary shape for {@code GET /api/projets/{id}/tranches}.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code CfgTranche}. Adjust once a spec is available.
 */
public record TrancheListItemDto(
        UUID id,
        String code,
        String description,
        String ville,
        LocalDateTime dateLivraison
) {
}
