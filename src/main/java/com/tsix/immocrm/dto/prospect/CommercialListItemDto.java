package com.tsix.immocrm.dto.prospect;

import java.util.UUID;

/**
 * Summary shape for {@code GET /api/commerciaux}.
 *
 * Source confirmed with the user: {@code CfgTier} filtered by {@code CfgTierKind.code} IN
 * (COM, AGC, ATC, ACC, RC) — commercial staff assignable to a Dossier/Prospection, including
 * "attachée commerciale [et communication]" roles.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code CfgTier}. Adjust once a spec is available.
 */
public record CommercialListItemDto(
        UUID id,
        String code,
        String prenom,
        String nom,
        String email,
        String telephone,
        String kind
) {
}
