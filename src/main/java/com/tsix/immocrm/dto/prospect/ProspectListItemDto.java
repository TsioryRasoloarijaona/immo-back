package com.tsix.immocrm.dto.prospect;

import java.util.UUID;

/**
 * Summary shape for {@code GET /api/prospects}.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code CfgTier}. Adjust once a spec is available.
 */
public record ProspectListItemDto(
        UUID id,
        String code,
        String prenom,
        String nom,
        String raisonSociale,
        String telephone,
        String mobile,
        String email,
        String ville,
        String type
) {
}
