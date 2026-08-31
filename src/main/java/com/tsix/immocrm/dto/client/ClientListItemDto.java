package com.tsix.immocrm.dto.client;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Summary shape for {@code GET /api/clients} — a "client" is any {@code CfgTier} referenced by
 * at least one {@code ComFolder.CfgCustomerId} (regardless of {@code CfgTierType}/{@code
 * CfgTierKind}), distinct from {@code /api/prospects} which filters by {@code IsProspect}.
 * {@code totalAPayer}/{@code totalRegle}/{@code totalReste} are aggregated server-side across
 * every dossier of the client — not derivable client-side without one call per row.
 */
public record ClientListItemDto(
        UUID id,
        String code,
        String nomAffiche,
        String mobile,
        String telephone,
        String email,
        String ville,
        Integer nbDossiers,
        BigDecimal totalAPayer,
        BigDecimal totalRegle,
        BigDecimal totalReste
) {
}
